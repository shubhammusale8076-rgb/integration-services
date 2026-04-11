package com.integration_service.dto.configDto;

import lombok.Data;

@Data
public class WhatsAppConfig {

    private String accessToken;
    private String phoneNumberId;
    private String version;
    private String webhookVerifyToken;
}
