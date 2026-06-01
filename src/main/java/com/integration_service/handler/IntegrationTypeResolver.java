package com.integration_service.handler;

import com.integration_service.communication.entity.IntegrationType;

public final class IntegrationTypeResolver {

    private IntegrationTypeResolver() {
    }

    public static IntegrationType configKey(IntegrationType handlerType) {
        return switch (handlerType) {
            case GMAIL, GOOGLE_Calendar -> IntegrationType.GOOGLE;
            default -> handlerType;
        };
    }
}
