package com.integration_service.common.config;

public class RequestContext {

    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentRequestId = new ThreadLocal<>();

    public static void setUserId(String userId) {
        currentUserId.set(userId);
    }

    public static String getUserId() {
        return currentUserId.get();
    }

    public static void setRequestId(String requestId) {
        currentRequestId.set(requestId);
    }

    public static String getRequestId() {
        return currentRequestId.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentRequestId.remove();
    }
}
