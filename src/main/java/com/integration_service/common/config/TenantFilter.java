package com.integration_service.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import com.integration_service.common.constants.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith(SecurityConstants.WEBHOOKS_PATH_PREFIX) || path.startsWith(SecurityConstants.API_WEBHOOKS_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }


        if (path.equals(SecurityConstants.GOOGLE_CALLBACK_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader("X-Tenant-ID");

        if (tenantId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Tenant ID");
            return;
        }

        try {
            TenantContext.setTenant(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}