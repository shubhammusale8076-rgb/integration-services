package com.integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageRequest {

    private UUID tenantId;
    private String metaTemplateId;
    private String phoneNumber;
    private Map<String, String> variables;
}
