package com.integration_service.communication.client;

import com.integration_service.common.config.CorrelationContext;
import com.integration_service.common.constants.SecurityConstants;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreClientConfig {

    @Bean
    public RequestInterceptor coreClientRequestInterceptor(
            @Value("${internal.api.secret}") String internalSecret) {
        return template -> {
            template.header(SecurityConstants.HEADER_INTERNAL_SECRET, internalSecret);
            String correlationId = CorrelationContext.get();
            if (correlationId != null && !correlationId.isBlank()) {
                template.header(SecurityConstants.HEADER_CORRELATION_ID, correlationId);
            }
        };
    }
}
