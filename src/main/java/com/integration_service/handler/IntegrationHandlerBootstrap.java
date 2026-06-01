package com.integration_service.handler;

import com.integration_service.communication.entity.IntegrationType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationHandlerBootstrap {

    private final List<IntegrationHandler> handlers;

    @PostConstruct
    public void logRegisteredHandlers() {
        log.info("Registered {} IntegrationHandler(s)", handlers.size());

        for (IntegrationHandler handler : handlers) {
            IntegrationType configKey = IntegrationTypeResolver.configKey(handler.getService());
            log.info("  -> {} : {} (config key: {})",
                    handler.getClass().getSimpleName(), handler.getService(), configKey);
        }

        Map<IntegrationType, Long> byService = handlers.stream()
                .collect(Collectors.groupingBy(IntegrationHandler::getService, Collectors.counting()));

        byService.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> log.warn("Duplicate IntegrationHandler registration for type: {} (count: {})",
                        e.getKey(), e.getValue()));
    }
}
