package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntegrationSummaryResponse {

    private IntegrationType service;       // RAZORPAY
    private boolean enabled;
    private String mode;          // MANUAL / AUTOMATED / HYBRID
    private boolean connected;    // config exists or not
    private String status;        // CONNECTED, DISCONNECTED, FAILED
}
