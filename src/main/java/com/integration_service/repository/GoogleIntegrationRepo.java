package com.integration_service.repository;

import com.integration_service.entity.GoogleIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleIntegrationRepo extends JpaRepository<GoogleIntegration, String> {

    Optional<GoogleIntegration> findByTenantId(String tenantId);
}
