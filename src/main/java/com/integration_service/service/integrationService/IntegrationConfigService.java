package com.integration_service.service.integrationService;

import com.integration_service.common.config.TenantContext;
import com.integration_service.common.constants.Services;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.ResponseDto;
import com.integration_service.dto.integrationDto.IntegrationConfigResponse;
import com.integration_service.dto.integrationDto.IntegrationSummaryResponse;
import com.integration_service.dto.integrationDto.TenantIntegrationRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.mapper.IntegrationConfigMapper;
import com.integration_service.repository.IntegrationTemplateRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationConfigService {

    private final TenantIntegrationRepository repository;
    private final IntegrationTemplateRepo templateRepo;
    private final IntegrationConfigMapper integrationConfigMapper;
    private final ObjectMapper objectMapper;

    public ResponseDto save(TenantIntegrationRequest config) {

        String tenantIdStr = TenantContext.getTenant();
        UUID tenantId = UUID.fromString(tenantIdStr);

        Optional<TenantIntegration> existing = repository.findByTenantIdAndIntegrationType(tenantId, config.getService());
        
        TenantIntegration entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setEnabled(config.isEnabled());
            entity.setMode(config.getMode());
            entity.setMetadata(config.getConfigJson());
            entity.setUpdatedAt(LocalDateTime.now());
        } else {
            entity = integrationConfigMapper.toEntity(config, tenantIdStr);
            
            // Link to template definition
            templateRepo.findByService(config.getService()).ifPresent(entity::setTemplate);
        }

        if (Services.RAZORPAY.equals(config.getService())) {
            try {
                objectMapper.readValue(config.getConfigJson(), RazorpayConfig.class);
            } catch (Exception e) {
                throw new RuntimeException("Invalid Razorpay config JSON");
            }
        }

         repository.save(entity);

         return ResponseDto.builder().code(201).message("Integration Connected Successfully").build();
    }

    public List<IntegrationConfigResponse> getTenantConfigs() {
        return repository.findByTenantIdAndEnabledTrue(
                UUID.fromString(TenantContext.getTenant())
        ).stream()
                .map(integrationConfigMapper::toResponse)
                .toList();
    }

    public List<TenantIntegration> getTenantConfig() {
        return repository.findByTenantIdAndEnabledTrue(
                        UUID.fromString(TenantContext.getTenant())
                );
    }

    public Optional<TenantIntegration> getByService(IntegrationType service) {
        return repository.findByTenantIdAndIntegrationType(
                UUID.fromString(TenantContext.getTenant()),
                service
        );
    }

    public IntegrationConfigResponse getConfigByService(IntegrationType serviceName) {

        return repository.findByTenantIdAndIntegrationType(
                        UUID.fromString(TenantContext.getTenant()),
                        serviceName
                )
                .map(integrationConfigMapper::toResponse)
                .orElse(null);
    }

    public IntegrationSummaryResponse toggle(IntegrationType serviceName) {

        TenantIntegration config = repository
                .findByTenantIdAndIntegrationType(
                        UUID.fromString(TenantContext.getTenant()),
                        serviceName
                )
                .orElseThrow(() -> new RuntimeException("Config not found"));

        config.setEnabled(!config.isEnabled());
        config.setUpdatedAt(LocalDateTime.now());

        repository.save(config);

        return IntegrationSummaryResponse.builder()
                .service(serviceName)
                .enabled(config.isEnabled())
                .mode(config.getMode())
                .connected(true)
                .build();
    }

    public IntegrationSummaryResponse updateMode(IntegrationType serviceName, String mode) {

        TenantIntegration config = repository
                .findByTenantIdAndIntegrationType(
                        UUID.fromString(TenantContext.getTenant()),
                        serviceName
                )
                .orElseThrow(() -> new RuntimeException("Config not found"));

        config.setMode(mode);
        config.setUpdatedAt(LocalDateTime.now());

        repository.save(config);

        return IntegrationSummaryResponse.builder()
                .service(serviceName)
                .enabled(config.isEnabled())
                .mode(config.getMode())
                .connected(true)
                .build();
    }
}
