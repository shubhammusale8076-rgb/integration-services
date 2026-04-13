package com.integration_service.service;

import com.integration_service.config.TenantContext;
import com.integration_service.dto.EventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final DispatcherService dispatcherService;

    public void processEvent(EventRequest request) {
        dispatcherService.dispatch(request, TenantContext.getTenant());
    }
}
