package com.codelevel.module.identity.persistence.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TokenServiceIT {

    @Inject
    TokenService tokenService;

    @Test
    void shouldGenerateAccessTokenWithPublicIdAsSubject() {
        UUID publicId = UUID.randomUUID();
        String token = tokenService.generateAccessToken(publicId.toString(), "testuser", "ROLE_USER");

        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertTrue(payload.contains("\"sub\":\"" + publicId + "\""),
            "JWT payload should contain sub claim with publicId");
        assertFalse(payload.contains("userId"),
            "JWT payload must not expose internal database userId");
        assertFalse(payload.contains("email"),
            "JWT payload must not expose user email");
        assertFalse(payload.contains("fullName"),
            "JWT payload must not expose user fullName");
    }

    @Test
    void shouldGenerateAccessTokenWithRolesInGroupsClaim() {
        String token = tokenService.generateAccessToken(UUID.randomUUID().toString(), "testuser", "ROLE_ADMIN,ROLE_USER");

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertTrue(payload.contains("ROLE_ADMIN"), "JWT payload should contain ROLE_ADMIN");
        assertTrue(payload.contains("ROLE_USER"), "JWT payload should contain ROLE_USER");
    }

    @Test
    void shouldGenerateRefreshTokenAsNonNullUuidString() {
        String refreshToken = tokenService.generateRefreshToken();

        assertNotNull(refreshToken);
        assertDoesNotThrow(() -> UUID.fromString(refreshToken),
            "Refresh token should be a valid UUID string");
    }
}