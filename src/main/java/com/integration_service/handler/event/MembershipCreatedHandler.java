package com.integration_service.handler.event;

import com.integration_service.common.constants.EventTypes;
import com.integration_service.dto.EventRequest;
import com.integration_service.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipCreatedHandler implements EventHandler {

    private final DispatcherService dispatcherService;

    @Override
    public void handle(EventRequest request) {
        log.info("Processing MEMBERSHIP_CREATED for tenant: {}", request.getTenantId());

        // This handler triggers the standard integration fan-out
        dispatcherService.dispatch(request, request.getTenantId());
    }

    @Override
    public String getEventType() {
        return EventTypes.MEMBERSHIP_CREATED;
    }
}
