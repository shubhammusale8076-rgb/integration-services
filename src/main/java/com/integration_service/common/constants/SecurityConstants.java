package com.integration_service.common.constants;

public class SecurityConstants {
    public static final String GOOGLE_CALLBACK_PATH = "/api/google/callback";
    public static final String WEBHOOKS_PATH_PREFIX = "/webhooks/";
    public static final String API_WEBHOOKS_PATH_PREFIX = "/api/webhooks/";
    public static final String ACTUATOR_PATH_PREFIX = "/actuator/";

    public static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
}
