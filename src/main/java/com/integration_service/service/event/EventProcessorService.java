package com.integration_service.service.event;

import com.integration_service.dto.EventRequest;
import com.integration_service.handler.event.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventProcessorService {

    private final Map<String, EventHandler> handlerMap;

    public EventProcessorService(List<EventHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(EventHandler::getEventType, Function.identity()));
    }

    public void process(EventRequest request) {
        log.info("Received event: {} for tenant: {}", request.getEventType(), request.getTenantId());

        Optional.ofNullable(handlerMap.get(request.getEventType()))
                .ifPresentOrElse(
                        handler -> handler.handle(request),
                        () -> log.warn("No handler found for eventType: {}", request.getEventType())
                );
    }
}
