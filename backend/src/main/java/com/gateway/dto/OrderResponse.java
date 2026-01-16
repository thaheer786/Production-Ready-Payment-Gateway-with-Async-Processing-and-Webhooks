package com.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String id;
    
    @JsonProperty("merchant_id")
    private String merchantId;
    
    private Integer amount;
    private String currency;
    private String receipt;
    private String status;
    
    @JsonProperty("created_at")
    private String createdAt;
}
