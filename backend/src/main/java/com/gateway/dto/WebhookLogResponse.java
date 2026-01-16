package com.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookLogResponse {
    private String id;
    private String event;
    private String status;
    private Integer attempts;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("last_attempt_at")
    private String lastAttemptAt;
    
    @JsonProperty("response_code")
    private Integer responseCode;
}
