package com.integration_service.communication.dto;

import com.integration_service.communication.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorePaymentConfirmRequest {
    private UUID tenantId;
    private UUID memberId;
    private UUID membershipId;
    private String transactionId;
    private String correlationId;
    private String paymentId;
    private String orderId;
    private Double amount;
    private TransactionStatus paymentStatus;
    private String currency;
}
