package com.integration_service.integrations.google.controller;


import com.integration_service.communication.entity.WhatsAppDeliveryStatus;
import com.integration_service.dto.ResponseDto;
import com.integration_service.integrations.google.dto.GooglePasswordResetRequestDto;
import com.integration_service.integrations.google.dto.GoogleSheetExportRequest;
import com.integration_service.integrations.google.dto.GoogleSheetExportResponse;
import com.integration_service.integrations.google.service.GmailService;
import com.integration_service.integrations.google.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/google")
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleSheetsService  service;
    private final GmailService gmailService;

    @PostMapping("/export-members")
    public ResponseEntity<GoogleSheetExportResponse> exportMembers(@RequestBody GoogleSheetExportRequest request) {

        return ResponseEntity.ok(service.exportMembers(request));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ResponseDto> sendPasswordResetMessage(@RequestBody GooglePasswordResetRequestDto request) {

        ResponseDto status = gmailService.sendTemporaryPasswordMessage(request);

        return ResponseEntity.status(HttpStatus.OK).body(status);
    }
}
