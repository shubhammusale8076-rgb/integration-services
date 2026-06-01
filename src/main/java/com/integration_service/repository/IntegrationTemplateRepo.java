package com.integration_service.repository;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.entity.IntegrationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationTemplateRepo extends JpaRepository<IntegrationTemplate, UUID> {

    Optional<IntegrationTemplate> findByService(IntegrationType service);

    boolean existsByService(IntegrationType service);

    List<IntegrationTemplate> findByActiveTrue();
}
