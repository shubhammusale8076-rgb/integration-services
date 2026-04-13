package com.integration_service.controller;

import com.integration_service.config.TenantContext;
import com.integration_service.entity.GoogleIntegration;
import com.integration_service.repository.GoogleIntegrationRepo;
import com.integration_service.service.googleService.GoogleOAuthService;
import com.integration_service.service.googleService.GoogleTokenService;
import com.integration_service.service.googleService.GoogleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleOAuthService oauthService;
    private final GoogleTokenService tokenService;
    private final GoogleUserService userService;
    private final GoogleIntegrationRepo repository;

    // 🔗 STEP 1: Redirect user
    @GetMapping("/connect")
    public ResponseEntity<?> connect(@RequestParam(required = false) String tenantId) {

        if (tenantId == null) {
            tenantId = TenantContext.getTenant();
        }

        if (tenantId == null) {
            tenantId = "test_tenant"; // only for dev
        }

        String url = oauthService.getAuthUrl(tenantId);

        return ResponseEntity.ok(Map.of("authUrl", url));
    }

    // 🔁 STEP 2: Callback
    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam String code,
            @RequestParam String state) {

        String tenantId = state;

        Map<String, Object> tokenData =
                tokenService.exchangeCode(code);

        String accessToken = tokenData.get("access_token").toString();
        String refreshToken = tokenData.get("refresh_token").toString();
        Long expiresIn = Long.valueOf(tokenData.get("expires_in").toString());

        String email = userService.getEmail(accessToken);

        GoogleIntegration integration = repository
                .findByTenantId(tenantId)
                .orElse(new GoogleIntegration());

        integration.setTenantId(tenantId);
        integration.setEmail(email);
        integration.setAccessToken(accessToken);
        integration.setRefreshToken(refreshToken);
        integration.setExpiresIn(expiresIn);
        integration.setCreatedAt(LocalDateTime.now());

        repository.save(integration);

        return ResponseEntity.ok("Google connected successfully!");
    }
}
