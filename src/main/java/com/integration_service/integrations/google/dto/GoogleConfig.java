package com.integration_service.integrations.google.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleConfig {
    private String email;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
