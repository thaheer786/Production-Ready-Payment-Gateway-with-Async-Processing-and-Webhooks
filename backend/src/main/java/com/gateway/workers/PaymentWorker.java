package com.gateway.workers;

import com.gateway.config.AppConfig;
import com.gateway.jobs.ProcessPaymentJob;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.repository.MerchantRepository;
import com.gateway.repository.PaymentRepository;
import com.gateway.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentWorker {
    
    private final RedissonClient redissonClient;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookService webhookService;
    private final AppConfig appConfig;
    
    private static final String PAYMENT_QUEUE = "payment-jobs";
    private static final Random random = new Random();
    private volatile boolean running = true;
    
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread workerThread = new Thread(this::processJobs, "payment-worker");
        workerThread.setDaemon(false);
        workerThread.start();
        log.info("Payment worker started");
    }
    
    private void processJobs() {
        RBlockingQueue<ProcessPaymentJob> queue = redissonClient.getBlockingQueue(PAYMENT_QUEUE);
        
        while (running) {
            try {
                ProcessPaymentJob job = queue.poll(5, TimeUnit.SECONDS);
                
                if (job != null) {
                    processPayment(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing payment job", e);
            }
        }
    }
    
    private void processPayment(ProcessPaymentJob job) {
        try {
            log.info("Processing payment: {}", job.getPaymentId());
            
            // Small delay to ensure transaction is committed
            Thread.sleep(100);
            
            // Fetch payment
            Payment payment = paymentRepository.findById(job.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            // Simulate processing delay
            long delay;
            if (appConfig.isTestMode()) {
                delay = appConfig.getTestProcessingDelay();
            } else {
                delay = 5000 + random.nextInt(5001); // 5-10 seconds
            }
            Thread.sleep(delay);
            
            // Determine payment outcome
            boolean success;
            if (appConfig.isTestMode()) {
                success = appConfig.isTestPaymentSuccess();
            } else {
                // UPI: 90% success, Card: 95% success
                if ("upi".equals(payment.getMethod())) {
                    success = random.nextDouble() < 0.90;
                } else {
                    success = random.nextDouble() < 0.95;
                }
            }
            
            // Update payment status
            if (success) {
                payment.setStatus("success");
                log.info("Payment {} succeeded", payment.getId());
            } else {
                payment.setStatus("failed");
                payment.setErrorCode("PAYMENT_FAILED");
                payment.setErrorDescription("Payment processing failed");
                log.info("Payment {} failed", payment.getId());
            }
            
            payment = paymentRepository.save(payment);
            
            // Fetch merchant and enqueue webhook
            Merchant merchant = merchantRepository.findById(payment.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            
            String event = success ? "payment.success" : "payment.failed";
            webhookService.createWebhook(merchant, event, payment);
            
        } catch (Exception e) {
            log.error("Failed to process payment job: {}", job.getPaymentId(), e);
        }
    }
}
