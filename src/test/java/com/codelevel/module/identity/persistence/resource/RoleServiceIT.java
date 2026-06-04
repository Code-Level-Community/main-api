package com.codelevel.module.identity.persistence.resource;

import com.codelevel.module.identity.persistence.entity.PermissionEntity;
import com.codelevel.module.identity.persistence.entity.RoleEntity;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RoleServiceIT {

    @Inject
    RoleService roleService;

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void shouldAddRoleToUser() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);
        RoleEntity adminRole = RoleEntity.findByName("ROLE_ADMIN");
        assertNotNull(user);
        assertFalse(user.hasRole("ROLE_ADMIN"));

        roleService.addRoleToUser(user.getId(), "ROLE_ADMIN");
        entityManager.flush();
        entityManager.clear();

        UserEntity updated = UserEntity.findByUsername("user").orElse(null);
        assertNotNull(updated);
        assertTrue(updated.hasRole("ROLE_ADMIN"));
    }

    @Test
    @TestTransaction
    void shouldRemoveRoleFromUser() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);
        assertNotNull(user);
        assertTrue(user.hasRole("ROLE_USER"));

        roleService.removeRoleFromUser(user.getId(), "ROLE_USER");
        entityManager.flush();
        entityManager.clear();

        UserEntity updated = UserEntity.findByUsername("user").orElse(null);
        assertNotNull(updated);
        assertFalse(updated.hasRole("ROLE_USER"));
    }

    @Test
    @TestTransaction
    void shouldAddPermissionToRole() {
        RoleEntity role = RoleEntity.findByName("ROLE_USER");
        Set<String> permissionsBefore = role.getPermissions().stream()
            .map(PermissionEntity::getName)
            .collect(java.util.stream.Collectors.toSet());
        assertFalse(permissionsBefore.contains("users.read"));

        roleService.addPermissionToRole(role.getId(), "users.read");
        entityManager.flush();
        entityManager.clear();

        RoleEntity updated = RoleEntity.findByName("ROLE_USER");
        assertTrue(updated.getPermissions().stream()
            .anyMatch(p -> p.getName().equals("users.read")));
    }

    @Test
    @TestTransaction
    void shouldReturnTrueWhenUserHasPermission() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);
        RoleEntity role = RoleEntity.findByName("ROLE_USER");

        roleService.addPermissionToRole(role.getId(), "users.read");
        entityManager.flush();
        entityManager.clear();

        assertNotNull(user);
        assertTrue(roleService.userHasPermission(user.getId(), "users.read"));
    }

    @Test
    void shouldReturnFalseWhenUserLacksPermission() {
        UserEntity user = UserEntity.findByUsername("user").orElse(null);

        assertNotNull(user);
        assertFalse(roleService.userHasPermission(user.getId(), "users.delete"));
    }

    @Test
    void shouldReturnFalseWhenUserHasNoPermissions() {
        // Seeded users have roles but no permissions assigned to those roles
        UserEntity user = UserEntity.findByUsername("instructor").orElse(null);

        assertNotNull(user);
        assertFalse(roleService.userHasPermission(user.getId(), "users.read"));
    }

    @Test
    @TestTransaction
    void shouldReturnAllPermissionsAggregatedFromAllRoles() {
        UserEntity user = UserEntity.findByUsername("admin").orElse(null);
        RoleEntity adminRole = RoleEntity.findByName("ROLE_ADMIN");

        roleService.addPermissionToRole(adminRole.getId(), "users.read");
        roleService.addPermissionToRole(adminRole.getId(), "users.write");
        entityManager.flush();
        entityManager.clear();

        assertNotNull(user);
        Set<String> permissions = roleService.getUserPermissions(user.getId());
        assertTrue(permissions.contains("users.read"));
        assertTrue(permissions.contains("users.write"));
    }
}
