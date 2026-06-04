package com.codelevel.module.identity.persistence.entity;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RefreshTokenEntityIT {

    @Inject
    EntityManager entityManager;

    private RefreshTokenEntity buildToken(String username, boolean revoked, LocalDateTime expiresAt) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setToken(UUID.randomUUID().toString());
        token.setUsername(username);
        token.setRevoked(revoked);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    @TestTransaction
    void shouldReturnTrueWhenTokenIsNotRevokedAndNotExpired() {
        RefreshTokenEntity token = buildToken("user", false, LocalDateTime.now().plusDays(7));
        token.persistAndFlush();

        assertTrue(token.isValid());
    }

    @Test
    @TestTransaction
    void shouldReturnFalseWhenTokenIsRevoked() {
        RefreshTokenEntity token = buildToken("user", true, LocalDateTime.now().plusDays(7));
        token.persistAndFlush();

        assertFalse(token.isValid());
    }

    @Test
    @TestTransaction
    void shouldReturnFalseWhenTokenIsExpired() {
        RefreshTokenEntity token = buildToken("user", false, LocalDateTime.now().minusDays(1));
        token.persistAndFlush();

        assertFalse(token.isValid());
    }

    @Test
    @TestTransaction
    void shouldReturnFalseWhenTokenIsRevokedAndExpired() {
        RefreshTokenEntity token = buildToken("user", true, LocalDateTime.now().minusDays(1));
        token.persistAndFlush();

        assertFalse(token.isValid());
    }

    @Test
    @TestTransaction
    void shouldFindTokenByValue() {
        RefreshTokenEntity token = buildToken("admin", false, LocalDateTime.now().plusDays(7));
        token.persistAndFlush();
        String tokenValue = token.getToken();

        RefreshTokenEntity found = RefreshTokenEntity.findByToken(tokenValue).orElse(null);

        assertNotNull(found);
        assertEquals(tokenValue, found.getToken());
        assertEquals("admin", found.getUsername());
    }

    @Test
    @TestTransaction
    void shouldRevokeAllTokensByUsername() {
        String username = "testuser_" + UUID.randomUUID();
        RefreshTokenEntity t1 = buildToken(username, false, LocalDateTime.now().plusDays(7));
        RefreshTokenEntity t2 = buildToken(username, false, LocalDateTime.now().plusDays(3));
        t1.persistAndFlush();
        t2.persistAndFlush();
        entityManager.flush();

        RefreshTokenEntity.revokeByUsername(username);
        entityManager.clear();

        RefreshTokenEntity found1 = RefreshTokenEntity.findByToken(t1.getToken()).orElse(null);
        RefreshTokenEntity found2 = RefreshTokenEntity.findByToken(t2.getToken()).orElse(null);
        assertNotNull(found1);
        assertNotNull(found2);
        assertFalse(found1.isValid());
        assertFalse(found2.isValid());
    }

    @Test
    @TestTransaction
    void shouldNotThrowWhenRevokingByUsernameWithNoTokens() {
        assertDoesNotThrow(() ->
            RefreshTokenEntity.revokeByUsername("user_with_no_tokens_" + UUID.randomUUID()));
    }
}
