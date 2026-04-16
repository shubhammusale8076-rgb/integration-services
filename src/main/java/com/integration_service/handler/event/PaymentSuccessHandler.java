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
public class PaymentSuccessHandler implements EventHandler {

    private final DispatcherService dispatcherService;

    @Override
    public void handle(EventRequest request) {
        log.info("Processing PAYMENT_SUCCESS for tenant: {}", request.getTenantId());
        dispatcherService.dispatch(request, request.getTenantId());
    }

    @Override
    public String getEventType() {
        return EventTypes.PAYMENT_SUCCESS;
    }
}
