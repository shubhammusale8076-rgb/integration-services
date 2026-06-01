package com.integration_service.integrations.google.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.config.TenantContext;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.EventRequest;
import com.integration_service.handler.UnsupportedLifecycleHandler;
import com.integration_service.integrations.google.dto.GoogleConfig;
import com.integration_service.integrations.google.gmail.builder.EmailBuilder;
import com.integration_service.integrations.google.gmail.service.EmailTemplateService;
import com.integration_service.integrations.google.gmail.service.GmailClient;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailHandler implements UnsupportedLifecycleHandler {

    private final TenantIntegrationRepository repository;
    private final GoogleTokenService tokenService;
    private final GmailClient gmailClient;
    private final EmailTemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return true; // manual + automation
    }

    @Override
    public Object execute(EventRequest event, TenantIntegration config) {
        return executeWithRetry(event, config, 1);
    }

    private Object executeWithRetry(EventRequest event, TenantIntegration config, int retryCount) {
        String tenantIdStr = TenantContext.getTenant();
        UUID tenantId = UUID.fromString(tenantIdStr);

        TenantIntegration integration = repository.findByTenantIdAndIntegrationType(tenantId, IntegrationType.GOOGLE)
                .orElseThrow(() -> new RuntimeException("Google not connected"));

        GoogleConfig googleConfig;
        try {
            googleConfig = objectMapper.readValue(integration.getMetadata(), GoogleConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google config", e);
        }

        String accessToken = googleConfig.getAccessToken();

        try {
            Map<String, Object> data = event.getData();
            String rawMessage = buildMessage(event, data);

            gmailClient.sendEmail(accessToken, rawMessage);
            return "Email sent";

        } catch (Exception e) {
            if (retryCount > 0) {
                log.info("Refreshing Google token for tenant: {}", tenantIdStr);
                String newAccessToken = String.valueOf(tokenService.refreshAccessToken(googleConfig.getRefreshToken()));

                googleConfig.setAccessToken(newAccessToken);
                try {
                    integration.setMetadata(objectMapper.writeValueAsString(googleConfig));
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to serialize updated Google config", ex);
                }
                repository.save(integration);

                return executeWithRetry(event, config, retryCount - 1);
            }
            throw new RuntimeException("Gmail execution failed", e);
        }
    }

    private String buildMessage(EventRequest event, Map<String, Object> data) {
        String to;
        String subject;
        String html;

        if ("TRIAL_REMINDER".equals(event.getEventType())) {
            to = (String) data.get("email");
            subject = "Trial Reminder";
            html = templateService.process("email/trial-reminder", Map.of(
                    "name", data.get("name"),
                    "time", data.get("time")
            ));
            return EmailBuilder.buildHtml(to, subject, html);
        } else {
            to = (String) data.get("to");
            subject = (String) data.get("subject");
            String body = (String) data.get("body");
            return EmailBuilder.build(to, subject, body);
        }
    }

    @Override
    public IntegrationType getService() {

        return IntegrationType.GMAIL;
    }

    @Override
    public <T> T parseConfig(TenantIntegration integration, Class<T> clazz) {
        try {
            return objectMapper.readValue(integration.getMetadata(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google config", e);
        }
    }
}
