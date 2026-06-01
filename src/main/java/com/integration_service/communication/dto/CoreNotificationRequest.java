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
public class CoreNotificationRequest {
    private UUID tenantId;
    private UUID memberId;
    private Double amount;
    private Integer durationDays;
    private String paymentId;
}
