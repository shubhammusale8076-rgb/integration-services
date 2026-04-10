package com.integration_service.service.integrationService;

import com.integration_service.config.TenantContext;
import com.integration_service.dto.integrationDto.IntegrationSummaryResponse;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.repository.IntegrationTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationUIService {

    private final IntegrationTemplateRepo repository;

    private static final List<String> ALL_SERVICES = List.of(
            "RAZORPAY",
            "WHATSAPP"
    );

    public List<IntegrationSummaryResponse> getAllIntegrations() {

        String tenantId = TenantContext.getTenant();

        List<IntegrationTemplate> configs =
                repository.findByTenantId(tenantId);

        Map<String, IntegrationTemplate> configMap = configs.stream()
                .collect(Collectors.toMap(
                        IntegrationTemplate::getService,
                        c -> c,
                        (c1, c2) -> c1
                ));

        List<IntegrationSummaryResponse> response = new ArrayList<>();

        for (String service : ALL_SERVICES) {

            IntegrationTemplate config = configMap.get(service);

            if (config == null) {
                response.add(IntegrationSummaryResponse.builder()
                        .service(service)
                        .enabled(false)
                        .mode("MANUAL")
                        .connected(false)
                        .build());
            } else {
                response.add(IntegrationSummaryResponse.builder()
                        .service(service)
                        .enabled(config.isEnabled())
                        .mode(config.getMode())
                        .connected(true)
                        .build());
            }
        }

        return response;
    }
}
