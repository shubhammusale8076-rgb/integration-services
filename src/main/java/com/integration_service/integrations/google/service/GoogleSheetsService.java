package com.integration_service.integrations.google.service;


import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.integrations.google.dto.GoogleSheetExportRequest;
import com.integration_service.integrations.google.dto.GoogleSheetExportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GoogleSheetsService {

    private final TenantIntegrationRepository repository;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    public GoogleSheetExportResponse exportMembers(GoogleSheetExportRequest request) {

        try {

            // =====================================
            // FETCH GOOGLE CONNECTION
            // =====================================
            TenantIntegration integration = repository.findByTenantIdAndIntegrationType(request.getTenantId(), IntegrationType.GOOGLE)
                            .orElseThrow(() -> new RuntimeException("Google integration not connected"));

            // =====================================
            // GOOGLE CLIENT
            // =====================================
            UserCredentials credentials = UserCredentials.newBuilder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setAccessToken(new AccessToken(integration.getAccessToken(), null))
                            .setRefreshToken(integration.getRefreshToken())
                            .build();

            // =====================================
            // AUTO REFRESH TOKEN
            // =====================================

            credentials.refreshIfExpired();

            if (credentials.getAccessToken() != null) {
                integration.setAccessToken(credentials.getAccessToken().getTokenValue());

                integration.setUpdatedAt(LocalDateTime.now());
                repository.save(integration);
                log.info("Google access token refreshed successfully");
            }

            Sheets sheetsService = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                            .setApplicationName("Gym SaaS")
                            .build();

            // =====================================
            // CREATE SPREADSHEET
            // =====================================
            Spreadsheet spreadsheet = new Spreadsheet().setProperties(new SpreadsheetProperties().setTitle(request.getSheetTitle()));

            Spreadsheet createdSpreadsheet = sheetsService.spreadsheets()
                            .create(spreadsheet)
                            .execute();

            String spreadsheetId = createdSpreadsheet.getSpreadsheetId();

            // =====================================
            // BUILD SHEET DATA
            // =====================================
            List<List<Object>> values = new ArrayList<>();

            // HEADERS
            if (!request.getRows().isEmpty()) {

                values.add(
                        new ArrayList<>(
                                request.getRows()
                                        .get(0)
                                        .keySet()
                        )
                );

                // ROWS
                for (Map<String, Object> row : request.getRows()) {
                    values.add(new ArrayList<>(row.values()));
                }
            }

            ValueRange body = new ValueRange().setValues(values);

            // =====================================
            // INSERT DATA
            // =====================================
            sheetsService.spreadsheets()
                    .values()
                    .update(spreadsheetId, "Sheet1!A1", body)
                    .setValueInputOption("RAW")
                    .execute();

            String sheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId;

            // =====================================
            // RESPONSE
            // =====================================
            return GoogleSheetExportResponse
                    .builder()
                    .sheetId(spreadsheetId)
                    .sheetUrl(sheetUrl)
                    .message("Members exported successfully")
                    .build();

        } catch (Exception ex) {

            log.error("Failed to export members", ex);
            throw new RuntimeException("Google Sheets export failed");
        }
    }
}
