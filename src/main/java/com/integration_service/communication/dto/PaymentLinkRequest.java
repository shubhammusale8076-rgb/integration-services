package com.integration_service.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkRequest {
    private UUID tenantId;
    private UUID memberId;
    private UUID membershipId;
    private String transactionId;
    private String correlationId;
    private Double amount;
    private Integer durationDays;
    private String email;
    private String phone;
    private String memberName;
    private UUID planId;
    private String planName;
}
