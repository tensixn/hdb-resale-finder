package com.abcde.hdbresale.security;

import com.abcde.hdbresale.config.AppProperties;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and validates signed, short-lived access tokens (Section 3.4 item 3). */
@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(AppProperties properties) {
        byte[] secret = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.security.jwt-secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String issue(Long userId, String email, String sessionId, Instant expiresAt) {
        // TODO (Lab 4): implement issue
        throw new UnsupportedOperationException("TODO: JwtService.issue");
    }

    public Optional<AuthenticatedUser> parse(String token) {
        // TODO (Lab 4): implement parse
        throw new UnsupportedOperationException("TODO: JwtService.parse");
    }
}
