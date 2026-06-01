package com.integration_service.event;

import com.integration_service.dto.EventRequest;

public interface EventHandler {
    void handle(EventRequest request);
    String getEventType();
}
