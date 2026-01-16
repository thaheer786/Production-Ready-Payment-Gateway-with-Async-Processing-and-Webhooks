package com.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookLogsListResponse {
    private List<WebhookLogResponse> data;
    private Integer total;
    private Integer limit;
    private Integer offset;
}
