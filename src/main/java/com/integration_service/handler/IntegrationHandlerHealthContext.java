package com.integration_service.handler;

/**
 * Thread-local storage for the last health-check error on the current thread.
 */
public final class IntegrationHandlerHealthContext {

    private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

    private IntegrationHandlerHealthContext() {
    }

    public static void setLastError(String error) {
        LAST_ERROR.set(error);
    }

    public static String getLastError() {
        return LAST_ERROR.get();
    }

    public static void clear() {
        LAST_ERROR.remove();
    }
}
