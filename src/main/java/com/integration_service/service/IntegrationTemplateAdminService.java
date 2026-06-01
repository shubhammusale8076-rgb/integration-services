package com.integration_service.service;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.dto.integrationDto.IntegrationTemplateRequest;
import com.integration_service.dto.integrationDto.IntegrationTemplateResponse;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.repository.IntegrationTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationTemplateAdminService {

    private final IntegrationTemplateRepo integrationTemplateRepo;

    @Transactional
    public IntegrationTemplateResponse createIntegration(IntegrationTemplateRequest request) {
        if (integrationTemplateRepo.existsByService(request.getService())) {
            throw new IllegalArgumentException("Integration template for service " + request.getService() + " already exists.");
        }

        IntegrationTemplate template = new IntegrationTemplate();
        template.setService(request.getService());
        template.setDisplayName(request.getDisplayName());
        template.setDescription(request.getDescription());
        template.setIcon(request.getIcon());
        template.setIconColor(request.getIconColor());
        template.setIconBg(request.getIconBg());
        template.setAuthType(request.getAuthType());
        template.setConfigSchema(request.getConfigSchema());
        template.setActive(request.getActive());

        IntegrationTemplate saved = integrationTemplateRepo.save(template);
        return mapToResponse(saved);
    }

    @Transactional
    public IntegrationTemplateResponse updateIntegration(IntegrationType service, IntegrationTemplateRequest request) {
        IntegrationTemplate template = integrationTemplateRepo.findByService(service)
                .orElseThrow(() -> new IllegalArgumentException("Integration template not found for service: " + service));

        template.setDisplayName(request.getDisplayName());
        template.setDescription(request.getDescription());
        template.setIcon(request.getIcon());
        template.setIconColor(request.getIconColor());
        template.setIconBg(request.getIconBg());
        template.setAuthType(request.getAuthType());
        template.setConfigSchema(request.getConfigSchema());

        IntegrationTemplate updated = integrationTemplateRepo.save(template);
        return mapToResponse(updated);
    }

    @Transactional
    public IntegrationTemplateResponse toggleActiveStatus(IntegrationType service) {
        IntegrationTemplate template = integrationTemplateRepo.findByService(service)
                .orElseThrow(() -> new IllegalArgumentException("Integration template not found for service: " + service));

        template.setActive(!template.isActive());
        IntegrationTemplate updated = integrationTemplateRepo.save(template);
        return mapToResponse(updated);
    }

    public List<IntegrationTemplateResponse> getAllIntegrations() {
        return integrationTemplateRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public IntegrationTemplateResponse getIntegrationByService(IntegrationType service) {
        IntegrationTemplate template = integrationTemplateRepo.findByService(service)
                .orElseThrow(() -> new IllegalArgumentException("Integration template not found for service: " + service));
        return mapToResponse(template);
    }

    @Transactional
    public void softDeleteIntegration(IntegrationType service) {
        IntegrationTemplate template = integrationTemplateRepo.findByService(service)
                .orElseThrow(() -> new IllegalArgumentException("Integration template not found for service: " + service));

        template.setActive(false);
        integrationTemplateRepo.save(template);
    }

    private IntegrationTemplateResponse mapToResponse(IntegrationTemplate template) {
        return IntegrationTemplateResponse.builder()
                .id(template.getId())
                .service(template.getService())
                .displayName(template.getDisplayName())
                .description(template.getDescription())
                .icon(template.getIcon())
                .iconColor(template.getIconColor())
                .iconBg(template.getIconBg())
                .authType(template.getAuthType())
                .active(template.isActive())
                .configSchema(template.getConfigSchema())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
