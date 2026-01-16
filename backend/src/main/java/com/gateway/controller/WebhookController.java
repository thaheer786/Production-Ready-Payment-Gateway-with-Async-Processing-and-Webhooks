package com.gateway.controller;

import com.gateway.dto.WebhookLogResponse;
import com.gateway.dto.WebhookLogsListResponse;
import com.gateway.model.Merchant;
import com.gateway.model.WebhookLog;
import com.gateway.repository.WebhookLogRepository;
import com.gateway.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {
    
    private final WebhookLogRepository webhookLogRepository;
    private final JobQueueService jobQueueService;
    
    @GetMapping
    public ResponseEntity<WebhookLogsListResponse> getWebhooks(
            @RequestAttribute("merchant") Merchant merchant,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        int page = offset / limit;
        Page<WebhookLog> webhookPage = webhookLogRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchant.getId(), PageRequest.of(page, limit));
        
        List<WebhookLogResponse> data = webhookPage.getContent().stream()
                .map(this::toWebhookLogResponse)
                .collect(Collectors.toList());
        
        WebhookLogsListResponse response = new WebhookLogsListResponse(
                data,
                (int) webhookPage.getTotalElements(),
                limit,
                offset
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retryWebhook(
            @RequestAttribute("merchant") Merchant merchant,
            @PathVariable String id) {
        try {
            java.util.UUID webhookId = java.util.UUID.fromString(id);
            WebhookLog webhookLog = webhookLogRepository
                    .findByIdAndMerchantId(webhookId, merchant.getId())
                    .orElseThrow(() -> new RuntimeException("Webhook not found"));
            
            // Reset attempts and status
            webhookLog.setAttempts(0);
            webhookLog.setStatus("pending");
            webhookLog.setNextRetryAt(null);
            webhookLogRepository.save(webhookLog);
            
            // Enqueue webhook delivery job
            jobQueueService.enqueueWebhookJob(webhookLog.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", webhookLog.getId().toString());
            response.put("status", "pending");
            response.put("message", "Webhook retry scheduled");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    private WebhookLogResponse toWebhookLogResponse(WebhookLog log) {
        return WebhookLogResponse.builder()
                .id(log.getId().toString())
                .event(log.getEvent())
                .status(log.getStatus())
                .attempts(log.getAttempts())
                .createdAt(log.getCreatedAt().atZone(ZoneOffset.UTC).toString())
                .lastAttemptAt(log.getLastAttemptAt() != null ? 
                        log.getLastAttemptAt().atZone(ZoneOffset.UTC).toString() : null)
                .responseCode(log.getResponseCode())
                .build();
    }
}
