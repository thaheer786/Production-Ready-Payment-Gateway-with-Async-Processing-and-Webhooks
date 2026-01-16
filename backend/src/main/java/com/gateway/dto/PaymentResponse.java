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
public class PaymentResponse {
    private String id;
    
    @JsonProperty("order_id")
    private String orderId;
    
    private Integer amount;
    private String currency;
    private String method;
    private String vpa;
    
    @JsonProperty("card_number")
    private String cardNumber;
    
    private String status;
    private Boolean captured;
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_description")
    private String errorDescription;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
}
