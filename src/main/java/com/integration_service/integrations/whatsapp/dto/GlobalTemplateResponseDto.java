package com.integration_service.integrations.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalTemplateResponseDto {

    private UUID id;
    private String eventKey;
    private String templateName;
    private String category;
    private String languageCode;
    private String body;
    private Integer variableCount;
    private Boolean active;
}
