package com.integration_service.handler;

import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.dto.IntegrationValidationRequest;
import com.integration_service.communication.dto.IntegrationValidationResponse;
import com.integration_service.communication.entity.IntegrationHealthStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.EventRequest;

import java.time.LocalDateTime;
import java.util.Map;

public interface IntegrationHandler {

    IntegrationType getService();

    boolean supports(String eventType);

    Object execute(EventRequest event, TenantIntegration config);

    <T> T parseConfig(TenantIntegration integration, Class<T> clazz);

    void validate(Map<String, Object> config) throws Exception;

    void connect(Map<String, Object> config) throws Exception;

    IntegrationHealthResult validateHealth(TenantIntegration integration);

    default String getLastHealthError() {
        return IntegrationHandlerHealthContext.getLastError();
    }

    default boolean supportsHealthMonitoring() {
        return true;
    }

    default IntegrationValidationResponse validateIntegration(IntegrationValidationRequest request) {
        try {
            validate(request.getConfig());
            return IntegrationValidationResponse.builder()
                    .success(true)
                    .message("Validation successful")
                    .build();
        } catch (Exception ex) {

            return IntegrationValidationResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        }
    }

    default IntegrationHealthResult healthNotImplemented() {
        String error = "Health check not implemented for " + getService();
        IntegrationHandlerHealthContext.setLastError(error);
        return IntegrationHealthResult.builder()
                .healthy(false)
                .status(IntegrationHealthStatus.FAILED)
                .reauthRequired(false)
                .error(error)
                .checkedAt(LocalDateTime.now())
                .build();
    }
}
