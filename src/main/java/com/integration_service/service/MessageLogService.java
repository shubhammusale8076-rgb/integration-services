package com.integration_service.service;

import com.integration_service.config.TenantContext;
import com.integration_service.constants.Services;
import com.integration_service.entity.MessageLog;
import com.integration_service.repository.MessageLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageLogService {

    private final MessageLogRepo repository;
    private final ObjectMapper objectMapper;

    public MessageLog createPending(String phone, String template, Object request) {

        try {
            MessageLog log = MessageLog.builder()
                    .tenantId(TenantContext.getTenant())
                    .service(Services.WHATSAPP)
                    .phone(phone)
                    .template(template)
                    .status("PENDING")
                    .requestPayload(objectMapper.writeValueAsString(request))
                    .createdAt(LocalDateTime.now())
                    .build();

            return repository.save(log);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create message log", e);
        }
    }

    public void markSent(MessageLog log, String messageId, Object response) {

        try {
            log.setStatus("SENT");
            log.setMessageId(messageId);
            log.setResponse(objectMapper.writeValueAsString(response));

            repository.save(log);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update log", e);
        }
    }

    public void markFailed(MessageLog log, Exception ex) {
        log.setStatus("FAILED");
        log.setError(ex.getMessage());
        repository.save(log);
    }

    public void updateStatus(String messageId, String status) {

        MessageLog log = repository.findByMessageId(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        log.setStatus(status.toUpperCase());
        repository.save(log);
    }
}
