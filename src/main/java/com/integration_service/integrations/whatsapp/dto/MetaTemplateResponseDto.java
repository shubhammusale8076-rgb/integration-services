package com.integration_service.integrations.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaTemplateResponseDto {

    private List<MetaTemplateDto> data;
}
