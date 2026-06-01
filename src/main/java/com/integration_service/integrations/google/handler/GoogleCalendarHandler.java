package com.integration_service.integrations.google.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.config.TenantContext;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.EventRequest;
import com.integration_service.handler.UnsupportedLifecycleHandler;
import com.integration_service.integrations.google.calendar.service.CrmCalendarService;
import com.integration_service.integrations.google.dto.GoogleConfig;
import com.integration_service.integrations.google.calendar.service.BookingService;
import com.integration_service.integrations.google.calendar.service.CalendarEventBuilder;
import com.integration_service.integrations.google.calendar.service.GoogleCalendarClient;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleCalendarHandler implements UnsupportedLifecycleHandler {

    private final TenantIntegrationRepository repository;
    private final GoogleTokenService tokenService;
    private final GoogleCalendarClient calendarClient;
    private final BookingService bookingService;
    private final CrmCalendarService crmCalendarService;
    private final ObjectMapper objectMapper;

    @Override
    public IntegrationType getService() {
        return IntegrationType.GOOGLE_Calendar;
    }

    @Override
    public <T> T parseConfig(TenantIntegration integration, Class<T> clazz) {
        try {
            return objectMapper.readValue(integration.getMetadata(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google config", e);
        }
    }

    @Override
    public boolean supports(String eventType) {
        return "TRIAL_BOOKING".equals(eventType) || "CRM_TRIAL_BOOKING".equals(eventType);
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
        Map<String, Object> data = event.getData();

        if ("CRM_TRIAL_BOOKING".equals(event.getEventType())) {
            return crmCalendarService.createTrialBookingEvent(accessToken, data);
        }

        try {
            String summary = (String) data.get("summary");
            String description = (String) data.get("description");

            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");

            LocalDateTime start = LocalDateTime.parse((String) data.get("start"));
            LocalDateTime end = LocalDateTime.parse((String) data.get("end"));

            bookingService.validateSlot(tenantIdStr, start, end);

            Map<String, Object> eventBody = CalendarEventBuilder.build(
                    summary, description, start.toString(), end.toString(), email);

            String eventId = calendarClient.createEvent(accessToken, eventBody);

            bookingService.saveBooking(
                    tenantIdStr, name, email, phone, start, end, eventId
            );

            return "Event created";

        } catch (HttpClientErrorException.Unauthorized e) {
            if (retryCount > 0) {
                String newAccessToken = String.valueOf(tokenService.refreshAccessToken(
                        googleConfig.getRefreshToken()
                ));

                googleConfig.setAccessToken(newAccessToken);
                try {
                    integration.setMetadata(objectMapper.writeValueAsString(googleConfig));
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to serialize updated Google config", ex);
                }
                repository.save(integration);

                return executeWithRetry(event, config, retryCount - 1);
            }
            throw new RuntimeException("Google Calendar execution failed after retry", e);
        } catch (Exception e) {
            throw new RuntimeException("Google Calendar execution failed", e);
        }
    }
}
