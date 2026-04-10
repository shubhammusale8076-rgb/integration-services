package com.integration_service.mapper;

import com.integration_service.dto.integrationDto.IntegrationConfigResponse;
import com.integration_service.dto.integrationDto.IntegrationTemplateRequest;
import com.integration_service.entity.IntegrationTemplate;
import org.springframework.stereotype.Component;

@Component
public class IntegrationConfigMapper {

    public  IntegrationTemplate toEntity(IntegrationTemplateRequest dto) {
        IntegrationTemplate entity = new IntegrationTemplate();

        entity.setService(dto.getService());
        entity.setEnabled(dto.isEnabled());
        entity.setMode(dto.getMode());
        entity.setConfigSchema(dto.getConfigJson());

        return entity;
    }

    public  IntegrationConfigResponse toResponse(IntegrationTemplate entity) {
        return IntegrationConfigResponse.builder()
                .id(entity.getId())
                .service(entity.getService())
                .enabled(entity.isEnabled())
                .mode(entity.getMode())
                .configJson(entity.getConfigSchema())
                .build();
    }
}
