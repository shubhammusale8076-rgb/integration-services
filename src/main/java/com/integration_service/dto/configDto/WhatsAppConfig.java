package com.integration_service.dto.configDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppConfig {

    private String accessToken;
    private String phoneNumberId;
    private String version;
    private String webhookVerifyToken;
    private String businessAccountId;
}
