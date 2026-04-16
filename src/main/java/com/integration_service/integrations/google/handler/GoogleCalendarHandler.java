package com.integration_service.integrations.google.handler;

import com.integration_service.common.config.TenantContext;
import com.integration_service.dto.EventRequest;
import com.integration_service.integrations.google.entity.GoogleIntegration;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.repository.GoogleIntegrationRepo;
import com.integration_service.integrations.google.calendar.service.BookingService;
import com.integration_service.integrations.google.calendar.service.CalendarEventBuilder;
import com.integration_service.integrations.google.calendar.service.GoogleCalendarClient;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoogleCalendarHandler implements IntegrationHandler {

    private final GoogleIntegrationRepo repository;
    private final GoogleTokenService tokenService;
    private final GoogleCalendarClient calendarClient;
    private final BookingService bookingService;

    @Override
    public String getService() {
        return "GOOGLE_CALENDAR";
    }

    @Override
    public <T> T parseConfig(IntegrationTemplate template, Class<T> clazz) {
        return null;
    }

    @Override
    public boolean supports(String eventType) {
        return true;
    }

    @Override
    public Object execute(EventRequest event, IntegrationTemplate config) {
        return executeWithRetry(event, config, 1);
    }

    private Object executeWithRetry(EventRequest event, IntegrationTemplate config, int retryCount) {
        String tenantId = TenantContext.getTenant();

        GoogleIntegration integration = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Google not connected"));

        String accessToken = integration.getAccessToken();
        Map<String, Object> data = event.getData();

        try {
            String summary = (String) data.get("summary");
            String description = (String) data.get("description");

            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");

            LocalDateTime start = LocalDateTime.parse((String) data.get("start"));
            LocalDateTime end = LocalDateTime.parse((String) data.get("end"));

            bookingService.validateSlot(tenantId, start, end);

            Map<String, Object> eventBody = CalendarEventBuilder.build(
                    summary, description, start.toString(), end.toString(), email);

            String eventId = calendarClient.createEvent(accessToken, eventBody);

            bookingService.saveBooking(
                    tenantId, name, email, phone, start, end, eventId
            );

            return "Event created";

        } catch (HttpClientErrorException.Unauthorized e) {
            if (retryCount > 0) {
                String newAccessToken = tokenService.refreshAccessToken(
                        integration.getRefreshToken()
                );

                integration.setAccessToken(newAccessToken);
                repository.save(integration);

                return executeWithRetry(event, config, retryCount - 1);
            }
            throw new RuntimeException("Google Calendar execution failed after retry", e);
        } catch (Exception e) {
            throw new RuntimeException("Google Calendar execution failed", e);
        }
    }
}
