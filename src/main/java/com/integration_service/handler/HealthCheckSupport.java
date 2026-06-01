package com.integration_service.handler;

import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.entity.IntegrationHealthStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;

public final class HealthCheckSupport {

    private HealthCheckSupport() {
    }

    public static IntegrationHealthResult healthy() {
        return IntegrationHealthResult.builder()
                .healthy(true)
                .status(IntegrationHealthStatus.CONNECTED)
                .reauthRequired(false)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public static IntegrationHealthResult tokenExpired(String error) {
        IntegrationHandlerHealthContext.setLastError(error);
        return IntegrationHealthResult.builder()
                .healthy(false)
                .status(IntegrationHealthStatus.TOKEN_EXPIRED)
                .reauthRequired(true)
                .error(error)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public static IntegrationHealthResult reauthRequired(String error) {
        IntegrationHandlerHealthContext.setLastError(error);
        return IntegrationHealthResult.builder()
                .healthy(false)
                .status(IntegrationHealthStatus.REAUTH_REQUIRED)
                .reauthRequired(true)
                .error(error)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public static IntegrationHealthResult failed(String error) {
        IntegrationHandlerHealthContext.setLastError(error);
        return IntegrationHealthResult.builder()
                .healthy(false)
                .status(IntegrationHealthStatus.FAILED)
                .reauthRequired(false)
                .error(error)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public static IntegrationHealthResult fromThrowable(Throwable throwable) {
        if (isReauthRequired(throwable)) {
            return reauthRequired(resolveMessage(throwable));
        }
        if (isTokenExpired(throwable)) {
            return tokenExpired(resolveMessage(throwable));
        }
        return failed(resolveMessage(throwable));
    }

    public static boolean isTokenExpired(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        if (throwable instanceof WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                return true;
            }
            String body = ex.getResponseBodyAsString().toLowerCase();
            return containsTokenExpiredSignal(body);
        }
        return containsTokenExpiredSignal(message);
    }

    public static boolean isReauthRequired(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        return message.contains("invalid_grant")
                || message.contains("consent_required")
                || message.contains("revoked");
    }

    private static boolean containsTokenExpiredSignal(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("oauthexception")
                || lower.contains("invalid_token")
                || lower.contains("token expired")
                || lower.contains("expired")
                || lower.contains("session has expired")
                || lower.contains("error validating access token")
                || lower.contains("permission revoked");
    }

    private static String resolveMessage(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex && ex.getResponseBodyAsString() != null
                && !ex.getResponseBodyAsString().isBlank()) {
            return ex.getResponseBodyAsString();
        }
        return throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
    }
}
