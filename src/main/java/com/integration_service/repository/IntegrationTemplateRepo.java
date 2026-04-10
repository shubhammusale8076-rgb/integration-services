package com.integration_service.repository;

import com.integration_service.entity.IntegrationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationTemplateRepo extends JpaRepository<IntegrationTemplate, UUID> {

    List<IntegrationTemplate> findByTenantIdAndEnabledTrue(String tenantId);

    Optional<IntegrationTemplate> findByTenantIdAndService(String tenantId, String service);

    List<IntegrationTemplate> findByTenantId(String tenantId);
}
