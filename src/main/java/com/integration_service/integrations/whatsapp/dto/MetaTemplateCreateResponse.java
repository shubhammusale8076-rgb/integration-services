package com.integration_service.integrations.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaTemplateCreateResponse {

    private String id;

    private String status;

    private String category;
}
