package com.integration_service.repository;

import com.integration_service.entity.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationRepo extends JpaRepository<Integration, UUID> {
    Optional<Integration> findByTenantIdAndProvider(String tenant, String provider);

}

