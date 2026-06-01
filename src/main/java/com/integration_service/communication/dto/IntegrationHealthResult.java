package com.integration_service.communication.dto;

import com.integration_service.communication.entity.IntegrationHealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationHealthResult {

    private boolean healthy;

    private IntegrationHealthStatus status;

    private boolean reauthRequired;

    private String error;

    private LocalDateTime checkedAt;
}
