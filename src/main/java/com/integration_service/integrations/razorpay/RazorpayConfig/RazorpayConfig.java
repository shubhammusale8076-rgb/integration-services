package com.integration_service.integrations.razorpay.RazorpayConfig;

import lombok.Data;

@Data
public class RazorpayConfig {

    private String key;
    private String keySecret;
    private String webhookSecret;
}
