package com.integration_service.integrations.google.dto;


import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class GoogleSheetExportRequest {
    private UUID tenantId;
    private String sheetTitle;
    private List<Map<String, Object>> rows;
}
