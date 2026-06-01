package com.integration_service.integrations.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.entity.WhatsAppDeliveryStatus;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.WhatsAppMessageRequest;
import com.integration_service.dto.WhatsAppResponse;
import com.integration_service.dto.configDto.WhatsAppConfig;
import com.integration_service.integrations.whatsapp.dto.WelcomeMessage;
import com.integration_service.integrations.google.dto.GooglePasswordResetRequestDto;
import com.integration_service.integrations.whatsapp.entity.WhatsAppTemplate;
import com.integration_service.repository.WhatsAppTemplateRepository;
import com.integration_service.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.integration_service.integrations.whatsapp.entity.MessageLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppMessagingService {

    private final WhatsAppTemplateRepository templateRepository;
    private final TenantIntegrationRepository integrationRepository;
    private final WhatsAppClient whatsAppClient;
    private final ObjectMapper objectMapper;
    private final MessageLogService logService;
    private final ExecutionLogService executionLogService;

    public String sendTemplateMessage(WhatsAppMessageRequest request) {
        log.info("Processing WhatsApp template message request for tenant: {}, template: {}", 
                request.getTenantId(), request.getMetaTemplateId());

        // 1. Fetch template
        WhatsAppTemplate template = templateRepository.findByTenantIdAndMetaTemplateId(
                request.getTenantId(), request.getMetaTemplateId())
                .orElseThrow(() -> new RuntimeException("WhatsApp template not found: " + request.getMetaTemplateId()));

        if (!template.getActive()) {
            throw new RuntimeException("WhatsApp template is inactive: " + request.getMetaTemplateId());
        }

        // 3. Create Pending Log
        MessageLog messageLog = logService.createPending(request.getPhoneNumber(), request.getMetaTemplateId(), request);

        // 4. Fetch credentials
        TenantIntegration integration = integrationRepository.findByTenantIdAndIntegrationType(
                request.getTenantId(), IntegrationType.WHATSAPP)
                .orElseThrow(() -> new RuntimeException("WhatsApp integration not configured for tenant"));

        if (integration.getStatus() != IntegrationStatus.CONNECTED) {
            RuntimeException ex = new RuntimeException("WhatsApp integration is not connected");
            logService.markFailed(messageLog, ex);
            throw ex;
        }

        WhatsAppConfig config;
        try {
            config = objectMapper.readValue(integration.getMetadata(), WhatsAppConfig.class);
        } catch (Exception e) {
            log.error("Failed to parse WhatsApp config for tenant: {}", request.getTenantId());
            RuntimeException ex = new RuntimeException("Invalid WhatsApp configuration", e);
            logService.markFailed(messageLog, ex);
            throw ex;
        }

        // 5. Call WhatsApp API
        try {
            Map<String, Object> payload = Map.of(
                    "type", "text"
            );

            WhatsAppResponse response = whatsAppClient.sendMessage(config, request.getPhoneNumber(), payload);
            
            // 6. Log success
            String messageId = (response.getMessages() != null && !response.getMessages().isEmpty()) 
                    ? response.getMessages().get(0).getId() 
                    : "UNKNOWN";
            
            logService.markSent(messageLog, messageId, response);
            
            log.info("WhatsApp message sent successfully to {}. Response ID: {}", 
                    request.getPhoneNumber(), messageId);
            
            return "SUCCESS";

        } catch (Exception e) {
            log.error("WhatsApp API failure for tenant {}: {}", request.getTenantId(), e.getMessage());
            logService.markFailed(messageLog, e);
            return "FAILURE: " + e.getMessage();
        }
    }

    public WhatsAppDeliveryStatus sendWelcomeWhatsApp(WelcomeMessage request) {

        try{
            TenantIntegration whatsappIntegration = integrationRepository
                    .findByTenantIdAndIntegrationType(request.getTenantId(), IntegrationType.WHATSAPP)
                    .orElse(null);

            if (whatsappIntegration == null || whatsappIntegration.getStatus() != IntegrationStatus.CONNECTED) {
                log.warn("WhatsApp not connected for tenant {}, skipping link send", request.getTenantId());

                return WhatsAppDeliveryStatus.SKIPPED;
            }
            WhatsAppConfig config = objectMapper.readValue(whatsappIntegration.getMetadata(), WhatsAppConfig.class);

            Map<String, Object> payload = buildWelcomeTemplatePayload(request);

            WhatsAppResponse response = whatsAppClient.sendMessage(config, request.getPhoneNumber(), payload);

            executionLogService.logSuccess(
                    IntegrationType.WHATSAPP,
                    "Welcome Message",
                    Map.of( "correlationId", request.getCorrelationId()),
                    response
            );
            return WhatsAppDeliveryStatus.SENT;

        } catch (Exception ex) {
            log.error("Failed to send welcome message: {}",  ex.getMessage());
            executionLogService.logFailure(
                    IntegrationType.WHATSAPP,
                    "Welcome Message",
                    Map.of( "correlationId", request.getCorrelationId()),
                    ex

            );

            return WhatsAppDeliveryStatus.FAILED;
        }

    }

    private Map<String, Object> buildWelcomeTemplatePayload(WelcomeMessage request) {

        return Map.of(

                "type", "template",

                "template", Map.of(

                        "name",
                        "member_welcome_message",

                        "language", Map.of(
                                "code", "en"
                        ),

                        "components", List.of(

                                Map.of(
                                        "type", "body",

                                        "parameters", List.of(

                                                Map.of(
                                                        "type", "text",
                                                        "text",
                                                        request.getMemberName()
                                                ),

                                                Map.of(
                                                        "type", "text",
                                                        "text",
                                                        request.getPlanName()
                                                ),

                                                Map.of(
                                                        "type", "text",
                                                        "text",
                                                        request.getTrainerName()
                                                ),

                                                Map.of(
                                                        "type", "text",
                                                        "text",
                                                        request.getPlanStartDate()
                                                )
                                        )
                                )
                        )
                )
        );
    }


}
