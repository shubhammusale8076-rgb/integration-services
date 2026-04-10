package com.integration_service.service.integrationService;

import com.integration_service.config.TenantContext;
import com.integration_service.dto.integrationDto.IntegrationConfigResponse;
import com.integration_service.dto.integrationDto.IntegrationSummaryResponse;
import com.integration_service.dto.integrationDto.IntegrationTemplateRequest;
import com.integration_service.dto.ResponseDto;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.mapper.IntegrationConfigMapper;
import com.integration_service.repository.IntegrationTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegrationConfigService {

    private final IntegrationTemplateRepo repository;
    private final IntegrationConfigMapper integrationConfigMapper;

    public ResponseDto save(IntegrationTemplateRequest config) {

        IntegrationTemplate entity = integrationConfigMapper.toEntity(config);

        entity.setTenantId(TenantContext.getTenant());
        entity.setCreatedAt(LocalDateTime.now());

         repository.save(entity);

         return ResponseDto.builder().code(201).message("Integration Connected Successfully").build();
    }

    public List<IntegrationConfigResponse> getTenantConfigs() {
        return repository.findByTenantIdAndEnabledTrue(
                TenantContext.getTenant()
        ).stream()
                .map(integrationConfigMapper::toResponse)
                .toList();
    }

    public List<IntegrationTemplate> getTenantConfig() {
        return repository.findByTenantIdAndEnabledTrue(
                        TenantContext.getTenant()
                );
    }

    public Optional<IntegrationTemplate> getByService(String service) {
        return repository.findByTenantIdAndService(
                TenantContext.getTenant(),
                service
        );
    }

    public IntegrationConfigResponse getConfigByService(String serviceName) {

        return repository.findByTenantIdAndService(
                        TenantContext.getTenant(),
                        serviceName
                )
                .map(integrationConfigMapper::toResponse)
                .orElse(null);
    }

    public IntegrationSummaryResponse toggle(String serviceName) {

        IntegrationTemplate config = repository
                .findByTenantIdAndService(
                        TenantContext.getTenant(),
                        serviceName
                )
                .orElseThrow(() -> new RuntimeException("Config not found"));

        config.setEnabled(!config.isEnabled());

        repository.save(config);

        return IntegrationSummaryResponse.builder()
                .service(serviceName)
                .enabled(config.isEnabled())
                .mode(config.getMode())
                .connected(true)
                .build();
    }

    public IntegrationSummaryResponse updateMode(String serviceName, String mode) {

        IntegrationTemplate config = repository
                .findByTenantIdAndService(
                        TenantContext.getTenant(),
                        serviceName
                )
                .orElseThrow(() -> new RuntimeException("Config not found"));

        config.setMode(mode);

        repository.save(config);

        return IntegrationSummaryResponse.builder()
                .service(serviceName)
                .enabled(config.isEnabled())
                .mode(config.getMode())
                .connected(true)
                .build();
    }
}