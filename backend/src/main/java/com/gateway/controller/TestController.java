package com.gateway.controller;

import com.gateway.dto.JobQueueStatusResponse;
import com.gateway.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {
    
    private final JobQueueService jobQueueService;
    
    @GetMapping("/jobs/status")
    public ResponseEntity<JobQueueStatusResponse> getJobQueueStatus() {
        JobQueueStatusResponse response = JobQueueStatusResponse.builder()
                .pending(jobQueueService.getTotalPendingJobs())
                .processing(0)  // We don't track processing count in this implementation
                .completed(0)   // We don't track completed count in this implementation
                .failed(0)      // We don't track failed count in this implementation
                .workerStatus("running")
                .build();
        
        return ResponseEntity.ok(response);
    }
}
