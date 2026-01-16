package com.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.dto.*;
import com.gateway.model.IdempotencyKey;
import com.gateway.model.Merchant;
import com.gateway.model.Order;
import com.gateway.model.Payment;
import com.gateway.repository.IdempotencyKeyRepository;
import com.gateway.repository.MerchantRepository;
import com.gateway.repository.OrderRepository;
import com.gateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final JobQueueService jobQueueService;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    @Transactional
    public PaymentResponse createPayment(Merchant merchant, CreatePaymentRequest request, String idempotencyKey) {
        // Check idempotency key
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
                    .findByKeyAndMerchantId(idempotencyKey, merchant.getId());
            
            if (existingKey.isPresent()) {
                IdempotencyKey key = existingKey.get();
                if (key.getExpiresAt().isAfter(LocalDateTime.now())) {
                    // Return cached response
                    try {
                        return objectMapper.treeToValue(key.getResponse(), PaymentResponse.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize cached response", e);
                    }
                } else {
                    // Delete expired key
                    idempotencyKeyRepository.delete(key);
                }
            }
        }
        
        // Validate order exists
        Order order = orderRepository.findByIdAndMerchantId(request.getOrderId(), merchant.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Validate payment method
        if (!"upi".equals(request.getMethod()) && !"card".equals(request.getMethod())) {
            throw new RuntimeException("Invalid payment method");
        }
        
        if ("upi".equals(request.getMethod()) && (request.getVpa() == null || request.getVpa().isEmpty())) {
            throw new RuntimeException("VPA is required for UPI payments");
        }
        
        if ("card".equals(request.getMethod())) {
            if (request.getCardNumber() == null || request.getCardExpiry() == null || request.getCardCvv() == null) {
                throw new RuntimeException("Card details are required for card payments");
            }
        }
        
        // Generate payment ID
        String paymentId = generateId("pay_");
        
        // Create payment
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderId(order.getId());
        payment.setMerchantId(merchant.getId());
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setVpa(request.getVpa());
        payment.setCardNumber(request.getCardNumber());
        payment.setCardExpiry(request.getCardExpiry());
        payment.setCardCvv(request.getCardCvv());
        payment.setStatus("pending");
        payment.setCaptured(false);
        
        payment = paymentRepository.saveAndFlush(payment);
        
        // Enqueue job for async processing
        jobQueueService.enqueuePaymentJob(payment.getId());
        
        PaymentResponse response = toPaymentResponse(payment);
        
        // Store idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            try {
                IdempotencyKey key = new IdempotencyKey();
                key.setKey(idempotencyKey);
                key.setMerchantId(merchant.getId());
                key.setResponse(objectMapper.valueToTree(response));
                key.setExpiresAt(LocalDateTime.now().plusHours(24));
                idempotencyKeyRepository.save(key);
            } catch (Exception e) {
                log.error("Failed to save idempotency key", e);
            }
        }
        
        log.info("Created payment {} for order {}", paymentId, order.getId());
        
        return response;
    }
    
    @Transactional
    public PaymentResponse capturePayment(Merchant merchant, String paymentId, CapturePaymentRequest request) {
        Payment payment = paymentRepository.findByIdAndMerchantId(paymentId, merchant.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (!"success".equals(payment.getStatus())) {
            throw new RuntimeException("Payment not in capturable state");
        }
        
        if (payment.getCaptured()) {
            throw new RuntimeException("Payment already captured");
        }
        
        payment.setCaptured(true);
        payment = paymentRepository.save(payment);
        
        log.info("Captured payment {}", paymentId);
        
        return toPaymentResponse(payment);
    }
    
    public PaymentResponse getPayment(Merchant merchant, String paymentId) {
        Payment payment = paymentRepository.findByIdAndMerchantId(paymentId, merchant.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toPaymentResponse(payment);
    }
    
    public List<PaymentResponse> getPayments(Merchant merchant) {
        return paymentRepository.findByMerchantIdOrderByCreatedAtDesc(merchant.getId())
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }
    
    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .vpa(payment.getVpa())
                .cardNumber(payment.getCardNumber() != null ? maskCardNumber(payment.getCardNumber()) : null)
                .status(payment.getStatus())
                .captured(payment.getCaptured())
                .errorCode(payment.getErrorCode())
                .errorDescription(payment.getErrorDescription())
                .createdAt(payment.getCreatedAt().atZone(ZoneOffset.UTC).toString())
                .updatedAt(payment.getUpdatedAt() != null ? payment.getUpdatedAt().atZone(ZoneOffset.UTC).toString() : null)
                .build();
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        int visibleDigits = 4;
        int maskedLength = cardNumber.length() - visibleDigits;
        return "*".repeat(maskedLength) + cardNumber.substring(maskedLength);
    }
    
    private String generateId(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
