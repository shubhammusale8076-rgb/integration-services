package com.integration_service.service;

import com.integration_service.common.constants.Services;
import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.service.integrationService.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManualExecutionService {

    private final List<IntegrationHandler> handlers;
    private final IntegrationConfigService configService;
    private final ExecutionLogService logService;

    public Object execute(String service, Map<String, Object> data) {

        validate(service, data);

        IntegrationTemplate config = configService.getByService(service)
                .orElseThrow(() -> new RuntimeException("Integration not configured"));

        if (!config.isEnabled()) {
            throw new RuntimeException("Integration disabled");
        }

        if (!"MANUAL".equalsIgnoreCase(config.getMode())
                && !"HYBRID".equalsIgnoreCase(config.getMode())) {
            throw new RuntimeException("Manual execution not allowed");
        }

        IntegrationHandler handler = handlers.stream()
                .filter(h -> h.getService().equals(service))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Handler not found"));

        EventRequest event = new EventRequest();
        event.setEventType("MANUAL_TRIGGER");
        event.setTenantId(com.integration_service.common.config.TenantContext.getTenant());
        event.setPayload(data);

        try {

            Object response = handler.execute(event, config);

            logService.logSuccess(service, "MANUAL_TRIGGER", data, response);

            return response;

        } catch (Exception ex) {

            logService.logFailure(service, "MANUAL_TRIGGER", data, ex);

            throw ex;
        }
    }

    private void validate(String service, Map<String, Object> data) {

        if (Services.RAZORPAY.equals(service)) {

            if (!data.containsKey("amount")) {
                throw new RuntimeException("Amount is required");
            }

            if (!data.containsKey("phone")) {
                throw new RuntimeException("Phone is required");
            }
        }
    }
}
