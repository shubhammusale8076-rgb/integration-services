package com.integration_service.communication.repository;

import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantIntegrationRepository extends JpaRepository<TenantIntegration, UUID> {

    List<TenantIntegration> findByTenantId(UUID tenantId);

    Optional<TenantIntegration> findByTenantIdAndIntegrationType(UUID tenantId, IntegrationType integrationType);

    List<TenantIntegration> findByTenantIdAndEnabledTrue(UUID tenantId);

    List<TenantIntegration> findByStatusAndEnabledTrue(IntegrationStatus status);
}
