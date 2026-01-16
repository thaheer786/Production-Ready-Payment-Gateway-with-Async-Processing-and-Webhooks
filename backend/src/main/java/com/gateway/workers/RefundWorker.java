package com.gateway.workers;

import com.gateway.jobs.ProcessRefundJob;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.model.Refund;
import com.gateway.repository.MerchantRepository;
import com.gateway.repository.PaymentRepository;
import com.gateway.repository.RefundRepository;
import com.gateway.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundWorker {
    
    private final RedissonClient redissonClient;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookService webhookService;
    
    private static final String REFUND_QUEUE = "refund-jobs";
    private static final Random random = new Random();
    private volatile boolean running = true;
    
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread workerThread = new Thread(this::processJobs, "refund-worker");
        workerThread.setDaemon(false);
        workerThread.start();
        log.info("Refund worker started");
    }
    
    private void processJobs() {
        RBlockingQueue<ProcessRefundJob> queue = redissonClient.getBlockingQueue(REFUND_QUEUE);
        
        while (running) {
            try {
                ProcessRefundJob job = queue.poll(5, TimeUnit.SECONDS);
                
                if (job != null) {
                    processRefund(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing refund job", e);
            }
        }
    }
    
    private void processRefund(ProcessRefundJob job) {
        try {
            log.info("Processing refund: {}", job.getRefundId());
            
            // Small delay to ensure transaction is committed
            Thread.sleep(100);
            
            // Fetch refund
            Refund refund = refundRepository.findById(job.getRefundId())
                    .orElseThrow(() -> new RuntimeException("Refund not found"));
            
            // Fetch payment
            Payment payment = paymentRepository.findById(refund.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            // Verify payment is in refundable state
            if (!"success".equals(payment.getStatus())) {
                log.error("Payment {} not in refundable state", payment.getId());
                return;
            }
            
            // Simulate refund processing delay (3-5 seconds)
            long delay = 3000 + random.nextInt(2001);
            Thread.sleep(delay);
            
            // Update refund status
            refund.setStatus("processed");
            refund.setProcessedAt(LocalDateTime.now());
            refund = refundRepository.save(refund);
            
            log.info("Refund {} processed", refund.getId());
            
            // Fetch merchant and enqueue webhook
            Merchant merchant = merchantRepository.findById(refund.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            
            webhookService.createRefundWebhook(merchant, "refund.processed", refund, payment);
            
        } catch (Exception e) {
            log.error("Failed to process refund job: {}", job.getRefundId(), e);
        }
    }
}
