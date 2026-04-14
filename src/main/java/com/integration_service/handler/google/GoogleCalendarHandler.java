package com.integration_service.handler.google;

import com.integration_service.config.TenantContext;
import com.integration_service.dto.EventRequest;
import com.integration_service.entity.GoogleIntegration;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.repository.GoogleIntegrationRepo;
import com.integration_service.service.BookingService;
import com.integration_service.service.googleService.CalendarEventBuilder;
import com.integration_service.service.googleService.GoogleCalendarClient;
import com.integration_service.service.googleService.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                    summary, description,  start.toString(), end.toString(), email);

            String eventId = calendarClient.createEvent(accessToken, eventBody);

            bookingService.saveBooking(
                    tenantId, name, email, phone, start, end, eventId
            );

            return "Event created";

        } catch (Exception e){

            String newAccessToken = tokenService.refreshAccessToken(
                    integration.getRefreshToken()
            );

            integration.setAccessToken(newAccessToken);
            repository.save(integration);

            return execute(event, config); // retry
        }
    }
}
