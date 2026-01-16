package com.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.config.AppConfig;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.model.Refund;
import com.gateway.model.WebhookLog;
import com.gateway.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    
    private final WebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;
    private final JobQueueService jobQueueService;
    private final AppConfig appConfig;
    
    @Transactional
    public void createWebhook(Merchant merchant, String event, Payment payment) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("event", event);
            payloadMap.put("timestamp", Instant.now().getEpochSecond());
            
            Map<String, Object> dataMap = new HashMap<>();
            Map<String, Object> paymentMap = new HashMap<>();
            paymentMap.put("id", payment.getId());
            paymentMap.put("order_id", payment.getOrderId());
            paymentMap.put("amount", payment.getAmount());
            paymentMap.put("currency", payment.getCurrency());
            paymentMap.put("method", payment.getMethod());
            if (payment.getVpa() != null) {
                paymentMap.put("vpa", payment.getVpa());
            }
            paymentMap.put("status", payment.getStatus());
            paymentMap.put("created_at", payment.getCreatedAt().atZone(ZoneOffset.UTC).toString());
            
            dataMap.put("payment", paymentMap);
            payloadMap.put("data", dataMap);
            
            JsonNode payload = objectMapper.valueToTree(payloadMap);
            
            WebhookLog webhookLog = new WebhookLog();
            webhookLog.setMerchantId(merchant.getId());
            webhookLog.setEvent(event);
            webhookLog.setPayload(payload);
            webhookLog.setStatus("pending");
            webhookLog.setAttempts(0);
            
            webhookLog = webhookLogRepository.saveAndFlush(webhookLog);
            
            // Enqueue webhook delivery job
            jobQueueService.enqueueWebhookJob(webhookLog.getId());
            
            log.info("Created webhook log {} for event {}", webhookLog.getId(), event);
        } catch (Exception e) {
            log.error("Failed to create webhook", e);
        }
    }
    
    @Transactional
    public void createRefundWebhook(Merchant merchant, String event, Refund refund, Payment payment) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("event", event);
            payloadMap.put("timestamp", Instant.now().getEpochSecond());
            
            Map<String, Object> dataMap = new HashMap<>();
            Map<String, Object> refundMap = new HashMap<>();
            refundMap.put("id", refund.getId());
            refundMap.put("payment_id", refund.getPaymentId());
            refundMap.put("amount", refund.getAmount());
            if (refund.getReason() != null) {
                refundMap.put("reason", refund.getReason());
            }
            refundMap.put("status", refund.getStatus());
            refundMap.put("created_at", refund.getCreatedAt().atZone(ZoneOffset.UTC).toString());
            if (refund.getProcessedAt() != null) {
                refundMap.put("processed_at", refund.getProcessedAt().atZone(ZoneOffset.UTC).toString());
            }
            
            dataMap.put("refund", refundMap);
            payloadMap.put("data", dataMap);
            
            JsonNode payload = objectMapper.valueToTree(payloadMap);
            
            WebhookLog webhookLog = new WebhookLog();
            webhookLog.setMerchantId(merchant.getId());
            webhookLog.setEvent(event);
            webhookLog.setPayload(payload);
            webhookLog.setStatus("pending");
            webhookLog.setAttempts(0);
            
            webhookLog = webhookLogRepository.saveAndFlush(webhookLog);
            
            // Enqueue webhook delivery job
            jobQueueService.enqueueWebhookJob(webhookLog.getId());
            
            log.info("Created webhook log {} for event {}", webhookLog.getId(), event);
        } catch (Exception e) {
            log.error("Failed to create webhook", e);
        }
    }
    
    public String generateSignature(String payload, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to generate HMAC signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    public long getRetryDelay(int attemptNumber) {
        if (appConfig.isWebhookRetryIntervalsTest()) {
            // Test mode: shorter intervals (in milliseconds)
            return switch (attemptNumber) {
                case 1 -> 0;      // 0 seconds
                case 2 -> 5000;   // 5 seconds
                case 3 -> 10000;  // 10 seconds
                case 4 -> 15000;  // 15 seconds
                case 5 -> 20000;  // 20 seconds
                default -> 0;
            };
        } else {
            // Production mode: standard intervals (in milliseconds)
            return switch (attemptNumber) {
                case 1 -> 0;           // Immediate
                case 2 -> 60000;       // 1 minute
                case 3 -> 300000;      // 5 minutes
                case 4 -> 1800000;     // 30 minutes
                case 5 -> 7200000;     // 2 hours
                default -> 0;
            };
        }
    }
}
