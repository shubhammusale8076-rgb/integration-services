package com.integration_service.dto;

import lombok.Data;

@Data
public class RazorpayConfig {

    private String keyId;
    private String keySecret;
    private String webhookSecret;
}
