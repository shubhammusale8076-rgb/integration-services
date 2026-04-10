package com.integration_service.service;

import com.integration_service.config.TenantContext;
import com.integration_service.entity.ExecutionLog;
import com.integration_service.repository.ExecutionLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionLogService {

    private final ExecutionLogRepo repository;
    private final ObjectMapper objectMapper;

    public void logSuccess(String service,
                           String eventType,
                           Object request,
                           Object response) {

        saveLog(service, eventType, "SUCCESS", request, response, null);
    }

    public void logFailure(String service,
                           String eventType,
                           Object request,
                           Exception ex) {

        saveLog(service, eventType, "FAILED", request, null, ex.getMessage());
    }

    private void saveLog(String service,
                         String eventType,
                         String status,
                         Object request,
                         Object response,
                         String error) {

        try {
            ExecutionLog log = ExecutionLog.builder()
                    .tenantId(TenantContext.getTenant())
                    .service(service)
                    .eventType(eventType)
                    .status(status)
                    .requestPayload(objectMapper.writeValueAsString(request))
                    .response(response != null ? objectMapper.writeValueAsString(response) : null)
                    .error(error)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(log);

        } catch (Exception e) {
            // fallback logging
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
}
