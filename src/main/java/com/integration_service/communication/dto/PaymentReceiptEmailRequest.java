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
public class PaymentReceiptEmailRequest {
    private UUID tenantId;
    private UUID paymentTransactionId;
    private String email;
    private String correlationId;
}
