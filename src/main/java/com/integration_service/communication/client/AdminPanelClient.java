package com.integration_service.communication.client;

import com.integration_service.integrations.whatsapp.dto.GlobalTemplateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "admin-panel",
        url = "${admin.panel.url}"
)
public interface AdminPanelClient {

    @GetMapping("/api/v1/admin/whatsapp/global-templates")
    List<GlobalTemplateResponseDto> getGlobalTemplates();
}
