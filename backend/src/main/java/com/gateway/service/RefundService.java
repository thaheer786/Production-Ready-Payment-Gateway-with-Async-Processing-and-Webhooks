package com.gateway.service;

import com.gateway.dto.CreateRefundRequest;
import com.gateway.dto.RefundResponse;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.model.Refund;
import com.gateway.repository.PaymentRepository;
import com.gateway.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final JobQueueService jobQueueService;
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    @Transactional
    public RefundResponse createRefund(Merchant merchant, String paymentId, CreateRefundRequest request) {
        // Find payment
        Payment payment = paymentRepository.findByIdAndMerchantId(paymentId, merchant.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        // Verify payment is refundable
        if (!"success".equals(payment.getStatus())) {
            throw new RuntimeException("Payment not in refundable state");
        }
        
        // Validate refund amount
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new RuntimeException("Invalid refund amount");
        }
        
        // Calculate total refunded amount
        Integer totalRefunded = refundRepository.getTotalRefundedAmount(paymentId);
        if (totalRefunded == null) {
            totalRefunded = 0;
        }
        
        // Check if refund amount exceeds available amount
        int availableAmount = payment.getAmount() - totalRefunded;
        if (request.getAmount() > availableAmount) {
            throw new RuntimeException("Refund amount exceeds available amount");
        }
        
        // Generate refund ID
        String refundId = generateId("rfnd_");
        
        // Create refund
        Refund refund = new Refund();
        refund.setId(refundId);
        refund.setPaymentId(paymentId);
        refund.setMerchantId(merchant.getId());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus("pending");
        
        refund = refundRepository.saveAndFlush(refund);
        
        // Enqueue job for async processing
        jobQueueService.enqueueRefundJob(refund.getId());
        
        log.info("Created refund {} for payment {}", refundId, paymentId);
        
        return toRefundResponse(refund);
    }
    
    public RefundResponse getRefund(Merchant merchant, String refundId) {
        Refund refund = refundRepository.findByIdAndMerchantId(refundId, merchant.getId())
                .orElseThrow(() -> new RuntimeException("Refund not found"));
        return toRefundResponse(refund);
    }
    
    private RefundResponse toRefundResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .createdAt(refund.getCreatedAt().atZone(ZoneOffset.UTC).toString())
                .processedAt(refund.getProcessedAt() != null ? 
                        refund.getProcessedAt().atZone(ZoneOffset.UTC).toString() : null)
                .build();
    }
    
    private String generateId(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
