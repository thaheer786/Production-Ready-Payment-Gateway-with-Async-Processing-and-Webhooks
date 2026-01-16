package com.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundResponse {
    private String id;
    
    @JsonProperty("payment_id")
    private String paymentId;
    
    private Integer amount;
    private String reason;
    private String status;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("processed_at")
    private String processedAt;
}
