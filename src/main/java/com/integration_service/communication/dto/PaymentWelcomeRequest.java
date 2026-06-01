package com.integration_service.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWelcomeRequest {
    private UUID tenantId;
    private UUID memberId;
    private String phone;
    private String templateCode;
    private String correlationId;
}
