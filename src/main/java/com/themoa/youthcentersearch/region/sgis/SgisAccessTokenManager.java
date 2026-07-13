package com.themoa.youthcentersearch.region.sgis;

import com.themoa.youthcentersearch.region.sgis.dto.SgisAuthenticationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SgisAccessTokenManager {
    private final SgisAuthenticationClient authenticationClient;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public SgisAccessTokenManager(SgisAuthenticationClient authenticationClient) {
        this.authenticationClient = authenticationClient;
    }

    public synchronized String accessToken() {
        if (accessToken == null || Instant.now().plusSeconds(60).isAfter(expiresAt)) {
            issue();
        }
        return accessToken;
    }

    public synchronized void invalidate() {
        accessToken = null;
        expiresAt = Instant.EPOCH;
    }

    private void issue() {
        SgisAuthenticationResponse response = authenticationClient.authenticate();
        accessToken = response.token();
        expiresAt = parseExpiresAt(response.timeout());
    }

    private Instant parseExpiresAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now().plusSeconds(1800);
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value > Instant.now().getEpochSecond()) {
                return Instant.ofEpochSecond(value);
            }
            return Instant.now().plusSeconds(Math.max(60, value));
        } catch (NumberFormatException ex) {
            return Instant.now().plusSeconds(1800);
        }
    }
}
