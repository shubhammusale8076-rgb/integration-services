package com.integration_service.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.config.CorrelationContext;
import com.integration_service.communication.dto.PaymentWelcomeRequest;
import com.integration_service.communication.entity.*;
import com.integration_service.communication.repository.PaymentTransactionRepository;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.WhatsAppMessageRequest;
import com.integration_service.dto.WhatsAppResponse;
import com.integration_service.dto.configDto.WhatsAppConfig;
import com.integration_service.integrations.whatsapp.service.MessageLogService;
import com.integration_service.integrations.whatsapp.service.WhatsAppClient;
import com.integration_service.integrations.whatsapp.service.WhatsAppMessagingService;
import com.integration_service.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommunicationService {

    private final PaymentTransactionRepository transactionRepository;
    private final TenantIntegrationRepository integrationRepository;
    private final WhatsAppClient whatsAppClient;
    private final WhatsAppMessagingService whatsAppMessagingService;
    private final ObjectMapper objectMapper;
    private final ExecutionLogService executionLogService;


    @Transactional
    public WhatsAppDeliveryStatus sendPaymentLink(UUID transactionId) {

        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found"));

        if (transaction.getMemberPhone() == null || transaction.getMemberPhone().isBlank()) {
            log.warn("Skipping WhatsApp payment link: no phone for transaction {}", transactionId);
            transaction.setWhatsappStatus(WhatsAppDeliveryStatus.SKIPPED);
            transaction.setLastError("Member phone missing");
            transactionRepository.save(transaction);
            return WhatsAppDeliveryStatus.SKIPPED;
        }

        try {
            TenantIntegration whatsappIntegration = integrationRepository
                    .findByTenantIdAndIntegrationType(transaction.getTenantId(), IntegrationType.WHATSAPP)
                    .orElse(null);

            if (whatsappIntegration == null || whatsappIntegration.getStatus() != IntegrationStatus.CONNECTED) {
                log.warn("WhatsApp not connected for tenant {}, skipping link send", transaction.getTenantId());
                transaction.setWhatsappStatus(WhatsAppDeliveryStatus.SKIPPED);
                transactionRepository.save(transaction);
                return WhatsAppDeliveryStatus.SKIPPED;
            }

            WhatsAppConfig config = objectMapper.readValue(whatsappIntegration.getMetadata(), WhatsAppConfig.class);
            
            Map<String, Object> payload = buildPaymentTemplatePayload(transaction);

            WhatsAppResponse response = whatsAppClient.sendMessage(config, transaction.getMemberPhone(), payload);

            transaction.setWhatsappStatus(WhatsAppDeliveryStatus.SENT);
            transaction.setWhatsappSentAt(LocalDateTime.now());
            transaction.setLastError(null);
            transactionRepository.save(transaction);

            executionLogService.logSuccess(
                    IntegrationType.WHATSAPP,
                    "PAYMENT_LINK_SEND",
                    Map.of("transactionId", transactionId, "correlationId", transaction.getCorrelationId()),
                    response
            );

            log.info("Payment link WhatsApp sent: transactionId={}, correlationId={}",
                    transactionId, transaction.getCorrelationId());
            return WhatsAppDeliveryStatus.SENT;

        } catch (Exception ex) {
            log.error("Failed to send payment link WhatsApp for transaction {}: {}",
                    transactionId, ex.getMessage());
            transaction.setWhatsappStatus(WhatsAppDeliveryStatus.FAILED);
            transaction.setLastError(ex.getMessage());
            transaction.setRetryCount(transaction.getRetryCount() != null ? transaction.getRetryCount() + 1 : 1);
            transactionRepository.save(transaction);

            executionLogService.logFailure(
                    IntegrationType.WHATSAPP,
                    "PAYMENT_LINK_SEND",
                    Map.of("transactionId", transactionId),
                    ex

            );
            return WhatsAppDeliveryStatus.FAILED;

        }
    }

    private Map<String, Object> buildPaymentTemplatePayload(PaymentTransaction transaction) {

        return Map.of(

                "type", "template",

                "template", Map.of(

                        // =====================================
                        // TEMPLATE NAME
                        // =====================================

                        "name", "payment_link_message",

                        // =====================================
                        // LANGUAGE
                        // =====================================

                        "language", Map.of("code", "en"),

                        // =====================================
                        // BODY VARIABLES
                        // =====================================

                        "components", List.of(

                                Map.of(
                                        "type", "body",

                                        "parameters", List.of(

                                                // {{1}} -> Member Name
                                                Map.of(
                                                        "type", "text",
                                                        "text", transaction.getMemberName()
                                                ),

                                                // {{2}} -> Payment Link
                                                Map.of(
                                                        "type", "text",
                                                        "text", transaction.getUniversalPaymentLink()
                                                )
                                        )
                                )
                        )
                )
        );
    }


  

}
