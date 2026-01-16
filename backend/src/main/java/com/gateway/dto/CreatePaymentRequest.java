package com.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {
    @JsonProperty("order_id")
    private String orderId;
    
    private String method;
    private String vpa;
    
    @JsonProperty("card_number")
    private String cardNumber;
    
    @JsonProperty("card_expiry")
    private String cardExpiry;
    
    @JsonProperty("card_cvv")
    private String cardCvv;
}
