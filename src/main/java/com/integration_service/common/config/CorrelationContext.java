package com.integration_service.common.config;

import java.util.UUID;

public final class CorrelationContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static void set(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static String get() {
        return CORRELATION_ID.get();
    }

    public static String getOrGenerate() {
        String existing = CORRELATION_ID.get();
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        CORRELATION_ID.set(generated);
        return generated;
    }

    public static void clear() {
        CORRELATION_ID.remove();
    }
}
