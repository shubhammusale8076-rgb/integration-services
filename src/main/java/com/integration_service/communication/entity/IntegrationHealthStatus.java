package com.integration_service.communication.entity;

public enum IntegrationHealthStatus {
    CONNECTED,
    DEGRADED,
    TOKEN_EXPIRED,
    REAUTH_REQUIRED,
    DISCONNECTED,
    FAILED
}
