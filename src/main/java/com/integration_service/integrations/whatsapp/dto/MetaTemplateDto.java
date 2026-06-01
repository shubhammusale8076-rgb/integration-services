package com.integration_service.integrations.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaTemplateDto {

    private String id;
    private String name;
    private String status;
    private String category;
    private String language;
}