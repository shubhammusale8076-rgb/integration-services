package com.integration_service.common.security;

import com.integration_service.common.config.CorrelationContext;
import com.integration_service.common.config.RequestContext;
import com.integration_service.common.config.TenantContext;
import com.integration_service.common.constants.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiSecurityFilter extends OncePerRequestFilter {

    @Value("${internal.api.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        log.info("Incoming request path: {}", path);

        // =========================================
        // PUBLIC ENDPOINTS BYPASS
        // =========================================
        if (isPublicEndpoint(path)) {

            log.info("Public endpoint detected. Skipping internal auth for: {}", path);

            filterChain.doFilter(request, response);
            return;
        }

        // =========================================
        // INTERNAL SECRET VALIDATION
        // =========================================
        String requestSecret =
                request.getHeader(SecurityConstants.HEADER_INTERNAL_SECRET);

        log.info("REQUEST SECRET: {}", requestSecret);
        log.info("EXPECTED SECRET: {}", internalSecret);

        if (requestSecret == null || !requestSecret.equals(internalSecret)) {

            log.warn(
                    "Unauthorized access attempt to {}. Invalid or missing internal secret.",
                    path
            );

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden: Invalid internal secret"
            );

            return;
        }

        log.info("Internal secret validated successfully.");

        // =========================================
        // CONTEXT HEADERS
        // =========================================
        String tenantId =
                request.getHeader(SecurityConstants.HEADER_TENANT_ID);

        String userId =
                request.getHeader(SecurityConstants.HEADER_USER_ID);

        String requestId =
                request.getHeader(SecurityConstants.HEADER_REQUEST_ID);

        String correlationId =
                request.getHeader(SecurityConstants.HEADER_CORRELATION_ID);

        log.info("Tenant ID: {}", tenantId);
        log.info("User ID: {}", userId);
        log.info("Request ID: {}", requestId);
        log.info("Correlation ID: {}", correlationId);

        // =========================================
        // SET CONTEXTS
        // =========================================
        if (tenantId != null) {
            TenantContext.setTenant(tenantId);
        }

        if (userId != null) {
            RequestContext.setUserId(userId);
        }

        if (requestId != null) {
            RequestContext.setRequestId(requestId);
        }

        if (correlationId != null && !correlationId.isBlank()) {
            CorrelationContext.set(correlationId);
        }

        try {

            log.info("Proceeding with protected request for path: {}", path);

            filterChain.doFilter(request, response);

        } finally {

            // =========================================
            // CLEAR CONTEXTS
            // =========================================
            TenantContext.clear();
            RequestContext.clear();
            CorrelationContext.clear();

            log.info("Cleared request contexts for path: {}", path);
        }
    }

    private boolean isPublicEndpoint(String path) {

        return path.startsWith(SecurityConstants.WEBHOOKS_PATH_PREFIX)
                || path.startsWith(SecurityConstants.API_WEBHOOKS_PATH_PREFIX)
                || path.startsWith(SecurityConstants.ACTUATOR_PATH_PREFIX)
                || path.equals(SecurityConstants.GOOGLE_CALLBACK_PATH);
    }
}