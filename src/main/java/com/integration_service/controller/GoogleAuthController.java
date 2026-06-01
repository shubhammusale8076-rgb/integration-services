package com.integration_service.controller;

import com.integration_service.common.config.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.integrations.google.auth.GoogleOAuthService;
import com.integration_service.integrations.google.auth.GoogleTokenService;
import com.integration_service.integrations.google.auth.GoogleUserService;
import com.integration_service.integrations.google.dto.GoogleTokenResponse;
import com.integration_service.repository.IntegrationTemplateRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleOAuthService oauthService;
    private final GoogleTokenService tokenService;
    private final GoogleUserService userService;
    private final TenantIntegrationRepository repository;
    private final IntegrationTemplateRepo templateRepo;
    private final ObjectMapper objectMapper;

    @Value("${frontend.app.url}")
    private String frontendUrl;

    @GetMapping("/connect")
    public ResponseEntity<?> connect(@RequestParam(required = false) String tenantId) {
        if (tenantId == null) {
            tenantId = TenantContext.getTenant();
        }
        
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Tenant ID is required"));
        }

        String url = oauthService.getAuthUrl(tenantId);
        return ResponseEntity.ok(Map.of("authUrl", url));
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam(required = false) String code,
                                 @RequestParam(required = false) String state,
                                 @RequestParam(required = false) String error) {
        
        if (error != null || code == null || state == null) {
            log.error("Google OAuth callback error: {}", error);
            return new RedirectView("/integrations?status=error");
        }

        try {
            // Validate state and extract tenantId
            String tenantIdStr = oauthService.validateAndExtractTenant(state);
            UUID tenantId = UUID.fromString(tenantIdStr);

            // Exchange code for tokens via DTO
            GoogleTokenResponse tokenResponse = tokenService.exchangeCode(code);
            String email = userService.getEmail(tokenResponse.getAccessToken());

            // Load or create integration
            TenantIntegration integration = repository
                    .findByTenantIdAndIntegrationType(tenantId, IntegrationType.GOOGLE)
                    .orElseGet(TenantIntegration::new);

            integration.setTenantId(tenantId);
            integration.setIntegrationType(IntegrationType.GOOGLE);
            integration.setStatus(IntegrationStatus.CONNECTED);
            integration.setEnabled(true);
            integration.setMode("AUTOMATED");
            
            // Set token data
            integration.setAccessToken(tokenResponse.getAccessToken());
            
            // Only update refresh token if Google provided a new one
            if (tokenResponse.getRefreshToken() != null) {
                integration.setRefreshToken(tokenResponse.getRefreshToken());
            }
            
            // Convert expires_in to absolute expiry time
            integration.setExpiryTime(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));

            // Set metadata JSON
            Map<String, Object> metadata = Map.of("email", email);
            integration.setMetadata(objectMapper.writeValueAsString(metadata));

            if (integration.getId() == null) {
                templateRepo.findByService(IntegrationType.GOOGLE).ifPresent(integration::setTemplate);
            }

            integration.markConnectedHealth();
            repository.save(integration);
            return new RedirectView(frontendUrl +"/integrations?status=connected");

        } catch (Exception e) {
            log.error("Failed to process Google callback", e);
            return new RedirectView(
                    frontendUrl +
                            "/integrations?status=error"
            );
        }
    }
}
