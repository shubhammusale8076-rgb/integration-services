package com.integration_service.communication.dto;

import com.integration_service.communication.entity.WhatsAppDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkResponse {
    private UUID integrationTransactionId;
    private String universalPaymentLink;
    private String razorpayOrderId;
    private String transactionId;
    private String correlationId;
    private WhatsAppDeliveryStatus whatsappStatus;
    private String whatsappError;
}
