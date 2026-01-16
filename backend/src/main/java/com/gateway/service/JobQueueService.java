package com.gateway.service;

import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.jobs.ProcessPaymentJob;
import com.gateway.jobs.ProcessRefundJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {
    
    private final RedissonClient redissonClient;
    
    private static final String PAYMENT_QUEUE = "payment-jobs";
    private static final String WEBHOOK_QUEUE = "webhook-jobs";
    private static final String REFUND_QUEUE = "refund-jobs";
    
    public void enqueuePaymentJob(String paymentId) {
        RQueue<ProcessPaymentJob> queue = redissonClient.getQueue(PAYMENT_QUEUE);
        ProcessPaymentJob job = new ProcessPaymentJob(paymentId);
        queue.add(job);
        log.info("Enqueued payment job for payment: {}", paymentId);
    }
    
    public void enqueueWebhookJob(UUID webhookLogId) {
        RQueue<DeliverWebhookJob> queue = redissonClient.getQueue(WEBHOOK_QUEUE);
        DeliverWebhookJob job = new DeliverWebhookJob(webhookLogId);
        queue.add(job);
        log.info("Enqueued webhook job for webhook log: {}", webhookLogId);
    }
    
    public void enqueueRefundJob(String refundId) {
        RQueue<ProcessRefundJob> queue = redissonClient.getQueue(REFUND_QUEUE);
        ProcessRefundJob job = new ProcessRefundJob(refundId);
        queue.add(job);
        log.info("Enqueued refund job for refund: {}", refundId);
    }
    
    public int getPendingPaymentJobs() {
        RQueue<ProcessPaymentJob> queue = redissonClient.getQueue(PAYMENT_QUEUE);
        return queue.size();
    }
    
    public int getPendingWebhookJobs() {
        RQueue<DeliverWebhookJob> queue = redissonClient.getQueue(WEBHOOK_QUEUE);
        return queue.size();
    }
    
    public int getPendingRefundJobs() {
        RQueue<ProcessRefundJob> queue = redissonClient.getQueue(REFUND_QUEUE);
        return queue.size();
    }
    
    public int getTotalPendingJobs() {
        return getPendingPaymentJobs() + getPendingWebhookJobs() + getPendingRefundJobs();
    }
}
