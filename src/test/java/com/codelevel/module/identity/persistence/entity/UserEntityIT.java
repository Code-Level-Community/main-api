package com.codelevel.module.identity.persistence.entity;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserEntityIT {

    @Inject
    EntityManager entityManager;

    private UserEntity buildEntity(String usernamePrefix) {
        UserEntity entity = new UserEntity();
        entity.setUsername(usernamePrefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        entity.setEmail(usernamePrefix + "_" + UUID.randomUUID() + "@test.com");
        entity.setPassword("hashed_password");
        return entity;
    }

    @Test
    @TestTransaction
    void shouldGeneratePublicIdOnPrePersistWhenNull() {
        UserEntity entity = buildEntity("noid");
        assertNull(entity.getPublicId());

        entity.persist();

        assertNotNull(entity.getPublicId());
    }

    @Test
    @TestTransaction
    void shouldSetEnabledTrueOnPrePersist() {
        UserEntity entity = buildEntity("enabled");
        entity.persist();

        assertTrue(entity.isEnabled());
    }

    @Test
    @TestTransaction
    void shouldAssignRoleUserOnPrePersistWhenNoRolesSet() {
        UserEntity entity = buildEntity("noroles");
        assertTrue(entity.getRoles().isEmpty());

        entity.persist();

        assertTrue(entity.hasRole("ROLE_USER"),
            "Entity should have ROLE_USER assigned by @PrePersist when no roles are set");
    }

    @Test
    @TestTransaction
    void shouldNotOverridePublicIdWhenAlreadySet() {
        UUID specificId = UUID.randomUUID();
        UserEntity entity = buildEntity("withid");
        entity.setPublicId(specificId);

        entity.persist();

        assertEquals(specificId, entity.getPublicId());
    }

    @Test
    void shouldFindUserByUsername() {
        UserEntity found = UserEntity.findByUsername("admin").orElse(null);

        assertNotNull(found);
        assertEquals("admin", found.getUsername());
        assertEquals("admin@codelevel.com", found.getEmail());
    }

    @Test
    void shouldReturnNullWhenFindByUsernameNotFound() {
        UserEntity found = UserEntity.findByUsername("username_that_does_not_exist_xyz").orElse(null);

        assertNull(found);
    }

    @Test
    void shouldReturnRolesAsCommaSeparatedString() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);
        assertNotNull(user);

        String roles = user.getRolesAsString();

        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    void shouldReturnTrueOnHasRoleWhenUserHasRole() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);

        assertNotNull(user);
        assertTrue(user.hasRole("ROLE_USER"));
    }

    @Test
    void shouldReturnFalseOnHasRoleWhenUserLacksRole() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);

        assertNotNull(user);
        assertFalse(user.hasRole("ROLE_ADMIN"));
    }

    @Test
    @TestTransaction
    void shouldGetAllPermissionsWithoutDuplicates() {
        UserEntity user = UserEntity.findByUsername("admin").orElse(null);
        RoleEntity adminRole = RoleEntity.findByName("ROLE_ADMIN");
        PermissionEntity readPerm = PermissionEntity.findByName("users.read");
        PermissionEntity writePerm = PermissionEntity.findByName("users.write");

        adminRole.getPermissions().add(readPerm);
        adminRole.getPermissions().add(writePerm);
        adminRole.persist();
        entityManager.flush();
        entityManager.clear();

        UserEntity refreshed = UserEntity.findByUsername("admin").orElse(null);
        assertNotNull(refreshed);

        java.util.Set<String> permissions = refreshed.getAllPermissions();

        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("users.read"));
        assertTrue(permissions.contains("users.write"));
    }

    @Test
    @TestTransaction
    void shouldReturnTrueOnHasPermissionWhenPermissionIsAssigned() {
        UserEntity user = UserEntity.findByUsername("admin").orElse(null);
        RoleEntity adminRole = RoleEntity.findByName("ROLE_ADMIN");
        PermissionEntity readPerm = PermissionEntity.findByName("users.read");

        adminRole.getPermissions().add(readPerm);
        adminRole.persist();
        entityManager.flush();
        entityManager.clear();

        UserEntity refreshed = UserEntity.findByUsername("admin").orElse(null);
        assertNotNull(refreshed);
        assertTrue(refreshed.hasPermission("users.read"));
    }
}
