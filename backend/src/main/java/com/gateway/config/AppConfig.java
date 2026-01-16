package com.gateway.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {
    
    @Value("${app.test-mode:false}")
    private boolean testMode;
    
    @Value("${app.test-processing-delay:1000}")
    private long testProcessingDelay;
    
    @Value("${app.test-payment-success:true}")
    private boolean testPaymentSuccess;
    
    @Value("${app.webhook-retry-intervals-test:false}")
    private boolean webhookRetryIntervalsTest;
}
