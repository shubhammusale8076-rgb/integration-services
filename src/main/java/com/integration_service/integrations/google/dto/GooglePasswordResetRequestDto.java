package com.integration_service.integrations.google.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GooglePasswordResetRequestDto {

    private UUID tenantId;

    private String email;

    private String memberName;

    private String temporaryPassword;
}