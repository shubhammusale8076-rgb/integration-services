package com.integration_service.dto;

import lombok.Data;

@Data
public class WhatsAppConfig {

    private String accessToken;
    private String phoneNumberId;
    private String webhookVerifyToken;
}

