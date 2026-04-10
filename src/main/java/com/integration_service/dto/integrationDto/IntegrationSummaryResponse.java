package com.integration_service.dto.integrationDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntegrationSummaryResponse {

    private String service;       // RAZORPAY
    private boolean enabled;
    private String mode;          // MANUAL / AUTOMATED / HYBRID
    private boolean connected;    // config exists or not
}
