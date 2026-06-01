package com.integration_service.integrations.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMetaTemplateRequestDto {

    private String name;
    private String category;
    private String language;
    private String body;
}
