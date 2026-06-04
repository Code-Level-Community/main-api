package com.codelevel.module.identity.persistence.resource;

import com.codelevel.module.identity.domain.PasswordHasher;
import com.codelevel.module.identity.domain.User;
import com.codelevel.module.identity.http.rest.dto.LoginResponse;
import com.codelevel.module.identity.http.rest.dto.RefreshRequest;
import com.codelevel.module.identity.persistence.entity.RefreshTokenEntity;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import com.codelevel.module.identity.persistence.resource.dto.UserSave;
import com.codelevel.shared.exception.ApplicationException;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AuthServiceIT {

    @Inject
    AuthService authService;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    EntityManager entityManager;

    // === login ===

    @Test
    @TestTransaction
    void shouldReturnLoginResponseWithTokensOnValidCredentials() {
        LoginResponse response = authService.login("admin", "admin");

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(900L, response.expiresIn());
    }

    @Test
    void shouldThrowBusinessRuleExceptionOnWrongPassword() {
        assertThrows(BusinessRuleException.class,
            () -> authService.login("admin", "wrongpassword"));
    }

    @Test
    void shouldThrowResourceNotFoundWhenUserDoesNotExist() {
        assertThrows(BusinessRuleException.class,
            () -> authService.login("nonexistentuser_xyz", "anypassword"));
    }

    @Test
    @TestTransaction
    void shouldRevokeOldRefreshTokenOnSecondLogin() {
        LoginResponse first = authService.login("user", "user");
        String firstToken = first.refreshToken();

        authService.login("user", "user");

        entityManager.clear();

        RefreshTokenEntity firstEntity = RefreshTokenEntity.findByToken(firstToken).orElse(null);
        assertNotNull(firstEntity);
        assertFalse(firstEntity.isValid());
    }

    // === refresh ===

    @Test
    @TestTransaction
    void shouldReturnNewAccessTokenOnValidRefresh() {
        LoginResponse login = authService.login("admin", "admin");
        LoginResponse refresh = authService.refresh(new RefreshRequest(login.refreshToken()));

        assertNotNull(refresh.accessToken());
        assertEquals(login.refreshToken(), refresh.refreshToken());
    }

    @Test
    void shouldThrowApplicationExceptionOnNonexistentRefreshToken() {
        assertThrows(ApplicationException.class,
            () -> authService.refresh(new RefreshRequest("token-that-does-not-exist-" + UUID.randomUUID())));
    }

    @Test
    @TestTransaction
    void shouldThrowApplicationExceptionOnRevokedRefreshToken() {
        String uniqueToken = "revoked-" + UUID.randomUUID();
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setToken(uniqueToken);
        token.setUsername("admin");
        token.setRevoked(true);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.persistAndFlush();

        assertThrows(ApplicationException.class,
            () -> authService.refresh(new RefreshRequest(uniqueToken)));
    }

    @Test
    @TestTransaction
    void shouldThrowApplicationExceptionOnExpiredRefreshToken() {
        String uniqueToken = "expired-" + UUID.randomUUID();
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setToken(uniqueToken);
        token.setUsername("admin");
        token.setRevoked(false);
        token.setCreatedAt(LocalDateTime.now().minusDays(8));
        token.setExpiresAt(LocalDateTime.now().minusDays(1));
        token.persistAndFlush();

        assertThrows(ApplicationException.class,
            () -> authService.refresh(new RefreshRequest(uniqueToken)));
    }

    // === logout ===

    @Test
    @TestTransaction
    void shouldRevokeRefreshTokensOnLogout() {
        LoginResponse login = authService.login("user", "user");
        UserEntity userEntity = UserEntity.findByUsername("user").orElse(null);

        assertNotNull(userEntity);
        authService.logout(userEntity.getPublicId());
        entityManager.clear();

        RefreshTokenEntity.findByToken(login.refreshToken())
            .ifPresent(token -> assertFalse(token.isValid()));
    }

    // === saveOrUpdate ===

    @Test
    @TestTransaction
    void shouldCreateUserWithHashedPassword() {
        String rawPassword = "mypassword123";
        User user = authService.saveOrUpdate(new UserSave("Test User", "testuser_" + UUID.randomUUID() + "@example.com", rawPassword));

        entityManager.clear();

        UserEntity entity = UserEntity.findByUsername(user.getUsername()).orElse(null);
        assertNotNull(entity);
        assertNotEquals(rawPassword, entity.getPassword());
        assertTrue(passwordHasher.verify(rawPassword, entity.getPassword()));
    }

    @Test
    @TestTransaction
    void shouldAssignRoleUserAutomaticallyOnCreate() {
        User user = authService.saveOrUpdate(new UserSave("New User", "newuser_" + UUID.randomUUID() + "@example.com", "password123"));

        entityManager.clear();

        UserEntity entity = UserEntity.findByUsername(user.getUsername()).orElse(null);
        assertNotNull(entity);
        assertTrue(entity.hasRole("ROLE_USER"));
    }

    @Test
    @TestTransaction
    void shouldThrowResourceAlreadyExistsOnDuplicateEmail() {
        assertThrows(ResourceAlreadyExists.class,
            () -> authService.saveOrUpdate(new UserSave("Admin Copy", "admin@codelevel.com", "password123")));
    }

    // === getUser ===

    @Test
    void shouldReturnUserByPublicId() {
        UserEntity adminEntity = UserEntity.findByUsername("admin").orElse(null);

        assertNotNull(adminEntity);
        User user = authService.getUser(adminEntity.getPublicId());

        assertEquals("admin", user.getUsername());
    }

    @Test
    void shouldThrowResourceNotFoundOnGetNonexistentUser() {
        assertThrows(ResourceNotFound.class,
            () -> authService.getUser(UUID.randomUUID()));
    }

    // === getAllUsers ===

    @Test
    @TestTransaction
    void shouldReturnOnlyEnabledUsersInGetAll() {
        User created = authService.saveOrUpdate(new UserSave("Temp User", "tempuser_" + UUID.randomUUID() + "@example.com", "password123"));
        entityManager.clear();

        authService.delete(created.getPublicId());
        entityManager.clear();

        Collection<User> users = authService.getAllUsers();

        boolean containsDeleted = users.stream()
            .anyMatch(u -> u.getPublicId().equals(created.getPublicId()));
        assertFalse(containsDeleted);
    }

    // === delete ===

    @Test
    @TestTransaction
    void shouldSoftDeleteUserKeepingRecordInDatabase() {
        User created = authService.saveOrUpdate(new UserSave("Delete Me", "deleteme_" + UUID.randomUUID() + "@example.com", "password123"));
        entityManager.clear();

        authService.delete(created.getPublicId());
        entityManager.clear();

        UserEntity entity = UserEntity.findByUsername(created.getUsername()).orElse(null);
        assertNotNull(entity);
        assertFalse(entity.isEnabled());
    }

    @Test
    void shouldThrowResourceNotFoundOnDeleteNonexistentUser() {
        assertThrows(ResourceNotFound.class,
            () -> authService.delete(UUID.randomUUID()));
    }
}
