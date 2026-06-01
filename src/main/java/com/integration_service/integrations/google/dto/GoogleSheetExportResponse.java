package com.integration_service.integrations.google.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleSheetExportResponse {
    private String sheetId;
    private String sheetUrl;
    private String message;
}
