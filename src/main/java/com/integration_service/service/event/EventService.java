package com.integration_service.service.event;

import com.integration_service.dto.EventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventProcessorService eventProcessorService;

    public void processEvent(EventRequest request) {
        // Delegate to the new event processing system
        eventProcessorService.process(request);
    }
}
