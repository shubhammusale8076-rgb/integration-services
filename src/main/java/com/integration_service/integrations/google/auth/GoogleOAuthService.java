package com.integration_service.integrations.google.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    // Fast in-memory state store (replace with Redis in multi-instance setups)
    private final Map<String, String> stateStore = new ConcurrentHashMap<>();

    public String getAuthUrl(String tenantId) {
        // Generate secure state: tenantId + UUID
        String state = tenantId + ":" + UUID.randomUUID().toString();
        stateStore.put(state, tenantId);

        String scope = String.join(" ",
                "https://www.googleapis.com/auth/gmail.send",
                "https://www.googleapis.com/auth/calendar",
                "https://www.googleapis.com/auth/spreadsheets",
                "https://www.googleapis.com/auth/userinfo.email"
        );

        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + scope
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + state;
    }

    public String validateAndExtractTenant(String state) {
        String tenantId = stateStore.remove(state);
        if (tenantId == null) {
            throw new IllegalArgumentException("Invalid or expired state parameter");
        }
        return tenantId;
    }
}
