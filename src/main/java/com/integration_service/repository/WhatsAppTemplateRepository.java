package com.integration_service.repository;

import com.integration_service.integrations.whatsapp.entity.WhatsAppTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, UUID> {

    Optional<WhatsAppTemplate> findByTenantIdAndMetaTemplateId(UUID tenantId, String templateCode);

    Optional<WhatsAppTemplate> findByMetaTemplateId(String metaTemplateId);

    Optional<WhatsAppTemplate> findByTenantIdAndEventKeyAndActiveTrue(UUID tenantId, String eventKey);

    List<WhatsAppTemplate> findByTenantId(UUID tenantId);
}
