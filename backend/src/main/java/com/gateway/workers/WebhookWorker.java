package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.config.AppConfig;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.model.Merchant;
import com.gateway.model.WebhookLog;
import com.gateway.repository.MerchantRepository;
import com.gateway.repository.WebhookLogRepository;
import com.gateway.service.JobQueueService;
import com.gateway.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookWorker {
    
    private final RedissonClient redissonClient;
    private final WebhookLogRepository webhookLogRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookService webhookService;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;
    
    private static final String WEBHOOK_QUEUE = "webhook-jobs";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private volatile boolean running = true;
    
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread workerThread = new Thread(this::processJobs, "webhook-worker");
        workerThread.setDaemon(false);
        workerThread.start();
        log.info("Webhook worker started");
    }
    
    private void processJobs() {
        RBlockingQueue<DeliverWebhookJob> queue = redissonClient.getBlockingQueue(WEBHOOK_QUEUE);
        
        while (running) {
            try {
                DeliverWebhookJob job = queue.poll(5, TimeUnit.SECONDS);
                
                if (job != null) {
                    deliverWebhook(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing webhook job", e);
            }
        }
    }
    
    private void deliverWebhook(DeliverWebhookJob job) {
        try {
            log.info("Delivering webhook: {}", job.getWebhookLogId());
            
            // Small delay to ensure transaction is committed
            Thread.sleep(100);
            
            // Fetch webhook log
            WebhookLog webhookLog = webhookLogRepository.findById(job.getWebhookLogId())
                    .orElseThrow(() -> new RuntimeException("Webhook log not found"));
            
            // Fetch merchant
            Merchant merchant = merchantRepository.findById(webhookLog.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            
            // Skip if webhook URL not configured
            if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isEmpty()) {
                log.info("Webhook URL not configured for merchant {}, skipping", merchant.getId());
                webhookLog.setStatus("success");  // Mark as success to avoid retries
                webhookLogRepository.save(webhookLog);
                return;
            }
            
            // Increment attempt counter
            webhookLog.setAttempts(webhookLog.getAttempts() + 1);
            webhookLog.setLastAttemptAt(LocalDateTime.now());
            
            // Generate payload and signature
            String payloadString = objectMapper.writeValueAsString(webhookLog.getPayload());
            String signature = webhookService.generateSignature(payloadString, merchant.getWebhookSecret());
            
            // Send HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(merchant.getWebhookUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadString))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            webhookLog.setResponseCode(response.statusCode());
            webhookLog.setResponseBody(response.body());
            
            // Check if successful
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                webhookLog.setStatus("success");
                log.info("Webhook {} delivered successfully", webhookLog.getId());
            } else {
                handleFailedDelivery(webhookLog);
            }
            
            webhookLogRepository.save(webhookLog);
            
        } catch (Exception e) {
            log.error("Failed to deliver webhook: {}", job.getWebhookLogId(), e);
            
            // Update webhook log on exception
            try {
                WebhookLog webhookLog = webhookLogRepository.findById(job.getWebhookLogId())
                        .orElse(null);
                if (webhookLog != null) {
                    webhookLog.setAttempts(webhookLog.getAttempts() + 1);
                    webhookLog.setLastAttemptAt(LocalDateTime.now());
                    webhookLog.setResponseBody(e.getMessage());
                    handleFailedDelivery(webhookLog);
                    webhookLogRepository.save(webhookLog);
                }
            } catch (Exception ex) {
                log.error("Failed to update webhook log after error", ex);
            }
        }
    }
    
    private void handleFailedDelivery(WebhookLog webhookLog) {
        if (webhookLog.getAttempts() >= 5) {
            // Max attempts reached, mark as permanently failed
            webhookLog.setStatus("failed");
            log.warn("Webhook {} failed after {} attempts", webhookLog.getId(), webhookLog.getAttempts());
        } else {
            // Schedule retry
            webhookLog.setStatus("pending");
            long retryDelayMs = webhookService.getRetryDelay(webhookLog.getAttempts() + 1);
            LocalDateTime nextRetry = LocalDateTime.now().plus(Duration.ofMillis(retryDelayMs));
            webhookLog.setNextRetryAt(nextRetry);
            
            // Enqueue for retry after delay
            Thread retryScheduler = new Thread(() -> {
                try {
                    Thread.sleep(retryDelayMs);
                    jobQueueService.enqueueWebhookJob(webhookLog.getId());
                    log.info("Scheduled retry for webhook {} (attempt {})", 
                            webhookLog.getId(), webhookLog.getAttempts() + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            retryScheduler.setDaemon(true);
            retryScheduler.start();
        }
    }
}
