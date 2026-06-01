package com.integration_service.integrations.google.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.EventRequest;
import com.integration_service.handler.HealthCheckSupport;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.handler.IntegrationHandlerHealthContext;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import com.integration_service.integrations.google.dto.GoogleConfig;
import com.integration_service.integrations.google.dto.GoogleTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Lifecycle handler for Google integrations. OAuth onboarding is handled by {@code GoogleAuthController}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;
    private final GoogleTokenService tokenService;

    @Override
    public IntegrationType getService() {
        return IntegrationType.GOOGLE;
    }

    @Override
    public boolean supports(String eventType) {
        return false;
    }

    @Override
    public Object execute(EventRequest event, TenantIntegration config) {
        throw new UnsupportedOperationException("Google execution is handled by GmailHandler and GoogleCalendarHandler");
    }

    @Override
    public <T> T parseConfig(TenantIntegration integration, Class<T> clazz) {
        try {
            if (integration.getMetadata() != null && !integration.getMetadata().isBlank()) {
                return objectMapper.readValue(integration.getMetadata(), clazz);
            }
            GoogleConfig config = GoogleConfig.builder()
                    .accessToken(integration.getAccessToken())
                    .refreshToken(integration.getRefreshToken())
                    .build();
            return objectMapper.convertValue(config, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google config", e);
        }
    }

    @Override
    public void validate(Map<String, Object> config) throws Exception {
        String clientId = (String) config.get("clientId");
        String clientSecret = (String) config.get("clientSecret");

        if (clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("Google clientId and clientSecret are required");
        }

        log.info("Google credentials validated successfully");
    }

    @Override
    public void connect(Map<String, Object> config) throws Exception {
        log.info("Google connected successfully");
    }

    @Override
    public IntegrationHealthResult validateHealth(TenantIntegration integration) {
        try {
            if (integration.getExpiryTime() != null && integration.getExpiryTime().isBefore(LocalDateTime.now())) {
                String refreshToken = integration.getRefreshToken();
                if (refreshToken == null || refreshToken.isBlank()) {
                    return HealthCheckSupport.tokenExpired("Google access token expired and no refresh token available");
                }
                try {
                    GoogleTokenResponse refreshed = tokenService.refreshAccessToken(refreshToken);
                    integration.setAccessToken(refreshed.getAccessToken());
                    integration.setExpiryTime(LocalDateTime.now().plusSeconds(refreshed.getExpiresIn()));
                    IntegrationHandlerHealthContext.setLastError(null);
                    return HealthCheckSupport.healthy();
                } catch (Exception refreshError) {
                    log.warn("Google token refresh failed for tenant {}: {}",
                            integration.getTenantId(), refreshError.getMessage());
                    return HealthCheckSupport.fromThrowable(refreshError);
                }
            }

            String accessToken = integration.getAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                GoogleConfig config = safeParseMetadata(integration);
                accessToken = config != null ? config.getAccessToken() : null;
            }

            if (accessToken == null || accessToken.isBlank()) {
                return HealthCheckSupport.reauthRequired("Google access token is missing");
            }

            IntegrationHandlerHealthContext.setLastError(null);
            return HealthCheckSupport.healthy();
        } catch (Exception ex) {
            log.warn("Google health check failed for tenant {}: {}", integration.getTenantId(), ex.getMessage());
            return HealthCheckSupport.fromThrowable(ex);
        }
    }

    private GoogleConfig safeParseMetadata(TenantIntegration integration) {
        try {
            if (integration.getMetadata() == null) {
                return null;
            }
            return objectMapper.readValue(integration.getMetadata(), GoogleConfig.class);
        } catch (Exception e) {
            return null;
        }
    }
}
