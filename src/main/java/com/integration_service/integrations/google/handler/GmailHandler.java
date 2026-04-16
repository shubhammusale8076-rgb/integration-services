package com.integration_service.integrations.google.handler;

import com.integration_service.common.config.TenantContext;
import com.integration_service.dto.EventRequest;
import com.integration_service.integrations.google.entity.GoogleIntegration;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.repository.GoogleIntegrationRepo;
import com.integration_service.integrations.google.gmail.builder.EmailBuilder;
import com.integration_service.integrations.google.gmail.service.EmailTemplateService;
import com.integration_service.integrations.google.gmail.service.GmailClient;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GmailHandler implements IntegrationHandler {

    private final GoogleIntegrationRepo repository;
    private final GoogleTokenService tokenService;
    private final GmailClient gmailClient;
    private final EmailTemplateService templateService;

    @Override
    public boolean supports(String eventType) {
        return true; // manual + automation
    }

    @Override
    public Object execute(EventRequest event, IntegrationTemplate config) {

        String tenantId = TenantContext.getTenant();

        GoogleIntegration integration = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Google not connected"));

        String accessToken = integration.getAccessToken();

        try {

            Map<String, Object> data = event.getData();

            String to;
            String subject;
            String rawMessage;

            // 🔥 1. TRIAL REMINDER (NEW)
            if ("TRIAL_REMINDER".equals(event.getEventType())) {

                to = (String) data.get("email");
                subject = "Trial Reminder";

                String html = templateService.process(
                        "email/trial-reminder",
                        Map.of(
                                "name", data.get("name"),
                                "time", data.get("time")
                        )
                );

                rawMessage = EmailBuilder.buildHtml(to, subject, html);

            }
            // 🔥 2. MANUAL / DEFAULT FLOW (EXISTING)
            else {

                to = (String) data.get("to");
                subject = (String) data.get("subject");
                String body = (String) data.get("body");

                rawMessage = EmailBuilder.build(to, subject, body);
            }

            gmailClient.sendEmail(accessToken, rawMessage);

            return "Email sent";

        } catch (Exception e) {

            // 🔥 TOKEN REFRESH (UNCHANGED LOGIC)
            String newAccessToken = tokenService.refreshAccessToken(
                    integration.getRefreshToken()
            );

            integration.setAccessToken(newAccessToken);
            repository.save(integration);

            Map<String, Object> data = event.getData();

            String to;
            String subject;
            String rawMessage;

            // 🔥 SAME LOGIC AGAIN (retry)
            if ("TRIAL_REMINDER".equals(event.getEventType())) {

                to = (String) data.get("email");
                subject = "Trial Reminder";

                String html = templateService.process(
                        "email/trial-reminder",
                        Map.of(
                                "name", data.get("name"),
                                "time", data.get("time")
                        )
                );

                rawMessage = EmailBuilder.buildHtml(to, subject, html);

            } else {

                to = (String) data.get("to");
                subject = (String) data.get("subject");
                String body = (String) data.get("body");

                rawMessage = EmailBuilder.build(to, subject, body);
            }

            gmailClient.sendEmail(newAccessToken, rawMessage);

            return "Email sent after token refresh";
        }
    }

    @Override
    public String getService() {
        return "Gmail";
    }

    @Override
    public <T> T parseConfig(IntegrationTemplate template, Class<T> clazz) {
        return null;
    }
}
