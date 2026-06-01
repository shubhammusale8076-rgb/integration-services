package com.integration_service.integrations.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.client.AdminPanelClient;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.configDto.WhatsAppConfig;
import com.integration_service.integrations.whatsapp.dto.*;
import com.integration_service.integrations.whatsapp.entity.WhatsAppTemplate;
import com.integration_service.repository.WhatsAppTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WhatsAppTemplateService {

    private final ObjectMapper objectMapper;
    private final WhatsAppClient whatsAppClient;
    private final AdminPanelClient adminPanelClient;
    private final WhatsAppTemplateRepository templateRepository;
    private final TenantIntegrationRepository integrationRepository;

    public void syncTemplates(UUID tenantId) {


      try{
          TenantIntegration integration = integrationRepository.findByTenantIdAndIntegrationType(tenantId, IntegrationType.WHATSAPP)
                          .orElseThrow(() -> new RuntimeException("WhatsApp integration not found."));

          if (
                  integration.getStatus() != IntegrationStatus.CONNECTED
          ) {
              throw new RuntimeException("WhatsApp integration not connected.");
          }

          WhatsAppConfig config = objectMapper.readValue(integration.getMetadata(), WhatsAppConfig.class);

          MetaTemplateResponseDto response = whatsAppClient.fetchTemplates(config);

          if (
                  response == null
                          || response.getData() == null
          ) {

              log.warn(
                      "No templates returned from Meta."
              );

              return;
          }

          for (MetaTemplateDto template : response.getData()) {

              WhatsAppTemplate entity = templateRepository.findByMetaTemplateId(template.getId())
                              .orElse(WhatsAppTemplate.builder().build());

              entity.setTenantId(tenantId);
              entity.setMetaTemplateId(template.getId());
              entity.setTemplateName(template.getName());
              entity.setLanguageCode(template.getLanguage());
              entity.setStatus(template.getStatus());
              entity.setActive("APPROVED".equalsIgnoreCase(template.getStatus()));

              templateRepository.save(entity);
          }

          log.info("WhatsApp templates synced successfully for tenant {}", tenantId);

      } catch (Exception ex) {
          log.error("Failed to sync WhatsApp templates: {}", ex.getMessage(), ex);

          throw new RuntimeException("Failed to sync WhatsApp templates.");
      }
    }

    public void provisionTemplates(UUID tenantId) {

        try {

            List<GlobalTemplateResponseDto> globalTemplates = adminPanelClient.getGlobalTemplates();

            TenantIntegration integration = integrationRepository
                            .findByTenantIdAndIntegrationType(tenantId, IntegrationType.WHATSAPP)
                            .orElseThrow(() -> new RuntimeException("WhatsApp integration missing."));

            WhatsAppConfig config = objectMapper.readValue(integration.getMetadata(), WhatsAppConfig.class);


            for (GlobalTemplateResponseDto template : globalTemplates) {

                CreateMetaTemplateRequestDto request = CreateMetaTemplateRequestDto
                                .builder()
                                .name(buildTenantTemplateName(tenantId, template.getTemplateName()))
                                .category(template.getCategory())
                                .language(template.getLanguageCode())
                                .body(template.getBody())
                                .build();

                MetaTemplateCreateResponse response = whatsAppClient.createTemplate(config, request);

                WhatsAppTemplate entity = WhatsAppTemplate.builder()

                                .tenantId(tenantId)
                                .globalTemplateId(template.getId())
                                .metaTemplateId(response.getId())
                                .templateName(request.getName())
                                .eventKey(template.getEventKey())
                                .status("PENDING")
                                .active(false)
                                .build();

                templateRepository.save(entity);
            }

        } catch (Exception ex) {

            log.error("Failed to provision templates", ex);

            throw new RuntimeException("Template provisioning failed.");
        }
    }

    private String buildTenantTemplateName(UUID tenantId, String templateName) {

        String shortTenant = tenantId.toString()
                        .substring(0, 8)
                        .replace("-", "");

        return templateName + "_" + shortTenant;
    }
}
