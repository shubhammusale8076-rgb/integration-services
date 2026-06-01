package com.integration_service.integrations.google.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import com.integration_service.integrations.google.dto.GoogleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmCalendarService {

    private final GoogleCalendarClient calendarClient;
    private final TenantIntegrationRepository integrationRepository;
    private final BookingService bookingService;
    private final GoogleTokenService tokenService;
    private final ObjectMapper objectMapper;

    public String createTrialBookingEvent(String tenantId, Map<String, Object> data) {
        return executeWithToken(tenantId, data, "TRIAL");
    }

    private String executeWithToken(String tenantId, Map<String, Object> data, String type) {
        TenantIntegration integration = integrationRepository.findByTenantIdAndIntegrationType(
                UUID.fromString(tenantId), IntegrationType.GOOGLE)
                .orElseThrow(() -> new RuntimeException("Google not connected for tenant: " + tenantId));

        GoogleConfig config;
        try {
            config = objectMapper.readValue(integration.getMetadata(), GoogleConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google config", e);
        }

        try {
            if ("TRIAL".equals(type)) {
                return createTrialBookingInternal(config.getAccessToken(), data);
            } else {
                return createStaffReminderInternal(config.getAccessToken(), data);
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("Token expired for tenant {}, refreshing...", tenantId);
            String newAccessToken = tokenService.refreshAccessToken(config.getRefreshToken()).getAccessToken();
            config.setAccessToken(newAccessToken);
            try {
                integration.setMetadata(objectMapper.writeValueAsString(config));
                integrationRepository.save(integration);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to update Google config", ex);
            }
            if ("TRIAL".equals(type)) {
                return createTrialBookingInternal(newAccessToken, data);
            } else {
                return createStaffReminderInternal(newAccessToken, data);
            }
        }
    }

    private String createTrialBookingInternal(String accessToken, Map<String, Object> data) {
        log.info("Creating CRM trial booking event");

        String leadName = (String) data.get("leadName");
        String leadEmail = (String) data.get("leadEmail");
        String trialType = (String) data.get("trialType");
        String startTime = (String) data.get("startTime");
        String endTime = (String) data.get("endTime");

        String summary = "Trial Session: " + trialType + " - " + leadName;
        String description = "CRM Automated Trial Booking for " + leadName + " (" + leadEmail + ")";

        Map<String, Object> eventBody = CalendarEventBuilder.build(
                summary,
                description,
                startTime,
                endTime,
                leadEmail
        );

        String eventId = calendarClient.createEvent(accessToken, eventBody);

        bookingService.saveBooking(
                (String) data.get("tenantId"),
                leadName,
                leadEmail,
                (String) data.get("leadPhone"),
                LocalDateTime.parse(startTime),
                LocalDateTime.parse(endTime),
                eventId
        );

        return eventId;
    }

    public String createStaffReminderEvent(String tenantId, Map<String, Object> data) {
        return executeWithToken(tenantId, data, "REMINDER");
    }

    private String createStaffReminderInternal(String accessToken, Map<String, Object> data) {
        log.info("Creating CRM staff reminder event");

        String staffEmail = (String) data.get("staffEmail");
        String task = (String) data.get("task");
        String startTime = (String) data.get("startTime");
        String endTime = (String) data.get("endTime");

        String summary = "CRM Task: " + task;
        String description = "Automated reminder for task: " + task;

        Map<String, Object> eventBody = CalendarEventBuilder.build(
                summary,
                description,
                startTime,
                endTime,
                staffEmail
        );

        return calendarClient.createEvent(accessToken, eventBody);
    }
}
