package com.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobQueueStatusResponse {
    private Integer pending;
    private Integer processing;
    private Integer completed;
    private Integer failed;
    
    @JsonProperty("worker_status")
    private String workerStatus;
}
