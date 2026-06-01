package com.integration_service.handler;

import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.entity.TenantIntegration;

import java.util.Map;

/**
 * Default lifecycle behavior for execution-only handlers (e.g. Gmail, Google Calendar).
 */
public interface UnsupportedLifecycleHandler extends IntegrationHandler {

    @Override
    default boolean supportsHealthMonitoring() {
        return false;
    }

    @Override
    default IntegrationHealthResult validateHealth(TenantIntegration integration) {
        return healthNotImplemented();
    }

    @Override
    default void validate(Map<String, Object> config) {
        throw new UnsupportedOperationException(
                "Validation is not supported for " + getService() + "; use the primary provider handler.");
    }

    @Override
    default void connect(Map<String, Object> config) {
        throw new UnsupportedOperationException(
                "Connect is not supported for " + getService() + "; use the primary provider handler.");
    }
}
