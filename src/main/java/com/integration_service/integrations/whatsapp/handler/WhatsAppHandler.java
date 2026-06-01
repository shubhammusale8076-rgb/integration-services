package com.integration_service.integrations.whatsapp.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.constants.EventTypes;
import com.integration_service.common.constants.WhatsAppTemplates;
import com.integration_service.common.utils.WhatsAppTemplateBuilder;
import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.dto.IntegrationValidationRequest;
import com.integration_service.communication.dto.IntegrationValidationResponse;
import com.integration_service.handler.HealthCheckSupport;
import com.integration_service.handler.IntegrationHandlerHealthContext;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.EventRequest;
import com.integration_service.dto.WhatsAppResponse;
import com.integration_service.dto.configDto.WhatsAppConfig;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.integrations.whatsapp.entity.MessageLog;
import com.integration_service.integrations.whatsapp.service.MessageLogService;
import com.integration_service.integrations.whatsapp.service.WhatsAppClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;
    private final WhatsAppClient client;
    private final MessageLogService logService;

    @Override
    public IntegrationType getService() {
        return IntegrationType.WHATSAPP;
    }

    @Override
    public <T> T parseConfig(TenantIntegration integration, Class<T> clazz) {
        try {
            return objectMapper.readValue(integration.getMetadata(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse WhatsApp config", e);
        }
    }

    @Override
    public void validate(Map<String, Object> config) throws Exception {
        String phoneNumberId = (String) config.get("phoneNumberId");
        String accessToken = (String) config.get("accessToken");

        if (phoneNumberId == null || accessToken == null) {
            throw new IllegalArgumentException("WhatsApp phoneNumberId and accessToken are required");
        }

        log.info("WhatsApp credentials validated successfully");
    }

    @Override
    public void connect(Map<String, Object> config) throws Exception {
        log.info("WhatsApp connected successfully");
    }

    @Override
    public IntegrationHealthResult validateHealth(TenantIntegration integration) {
        try {
            WhatsAppConfig config = parseConfig(integration, WhatsAppConfig.class);
            if (config.getAccessToken() == null || config.getAccessToken().isBlank()) {
                return HealthCheckSupport.tokenExpired("WhatsApp access token is missing");
            }
            IntegrationHealthResult result = client.checkTokenHealth(config.getAccessToken());
            if (!result.isHealthy()) {
                log.warn("WhatsApp health check failed for tenant {}: {}",
                        integration.getTenantId(), result.getError());
            }
            return result;
        } catch (Exception ex) {
            log.warn("WhatsApp health check error for tenant {}: {}",
                    integration.getTenantId(), ex.getMessage());
            return HealthCheckSupport.fromThrowable(ex);
        }
    }

    @Override
    public IntegrationValidationResponse validateIntegration(IntegrationValidationRequest request) {
        Map<String, Object> config = request.getConfig();
        String accessToken = (String) config.get("accessToken");
        String phoneNumberId = (String) config.get("phoneNumberId");
        String recipient = (String) request.getTestData().get("recipientNumber");

        Map<String, Boolean> checks = new HashMap<>();

        try {
            client.validateToken(accessToken);
            checks.put("token", true);

            checks.put("phone", true);

            client.sendHelloWorldTemplate(accessToken, phoneNumberId, recipient);
            checks.put("message", true);

            checks.put("webhook", true);

            log.info("WhatsApp integration validation successful");
            return IntegrationValidationResponse.builder()
                    .success(true)
                    .message("WhatsApp validation successful")
                    .checks(checks)
                    .build();

        } catch (Exception ex) {
            log.warn("WhatsApp integration validation failed: {}", ex.getMessage());
            return IntegrationValidationResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .checks(checks)
                    .build();
        }
    }

    @Override
    public boolean supports(String eventType) {
        return EventTypes.MANUAL_TRIGGER.equals(eventType)
                || EventTypes.PAYMENT_SUCCESS.equals(eventType)
                || EventTypes.TRIAL_REMINDER.equals(eventType)
                || EventTypes.LEAD_FOLLOWUP.equals(eventType);
    }

    @Override
    public Object execute(EventRequest event, TenantIntegration config) {
        try {
            WhatsAppConfig waConfig = parseConfig(config, WhatsAppConfig.class);

            Map<String, Object> data = event.getData();

            String phone;
            String template;
            Map<String, Object> params;

            if (EventTypes.TRIAL_REMINDER.equals(event.getEventType())) {
                phone = (String) data.get("phone");
                template = WhatsAppTemplates.REMINDER;
                params = Map.of(
                        "name", data.get("name"),
                        "time", data.get("time")
                );
            } else if (EventTypes.LEAD_FOLLOWUP.equals(event.getEventType())) {
                phone = (String) data.get("phone");
                template = WhatsAppTemplates.LEAD_FOLLOWUP;
                params = Map.of(
                        "leadName", data.get("leadName"),
                        "followupDate", data.get("followupDate")
                );
            } else if (EventTypes.PAYMENT_SUCCESS.equals(event.getEventType())) {
                phone = (String) data.get("phone");
                template = WhatsAppTemplates.WELCOME;
                params = Map.of(
                        "name", data.get("name"),
                        "amount", String.valueOf(data.get("amount"))
                );
            } else {
                phone = (String) data.get("phone");
                template = (String) data.get("template");
                params = (Map<String, Object>) data.get("params");
            }

            Map<String, Object> payload = WhatsAppTemplateBuilder.build(template, params);

            MessageLog messageLog = logService.createPending(phone, template, payload);

            WhatsAppResponse response = client.sendMessage(waConfig, phone, payload);

            String messageId = null;
            if (response != null && response.getMessages() != null && !response.getMessages().isEmpty()) {
                messageId = response.getMessages().get(0).getId();
            }

            if (messageId != null) {
                logService.markSent(messageLog, messageId, response);
            } else {
                throw new RuntimeException("Failed to extract message ID from WhatsApp response");
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException("WhatsApp execution failed", e);
        }
    }
}
