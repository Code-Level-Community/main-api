package com.codelevel.module.identity.persistence.resource;

import com.codelevel.module.identity.persistence.entity.PermissionEntity;
import com.codelevel.module.identity.persistence.entity.RoleEntity;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class RoleService {

    @Transactional
    public void addRoleToUser(Long userId, String roleName) {
        UserEntity user = UserEntity.findById(userId);
        RoleEntity role = RoleEntity.findByName(roleName);

        if (Objects.nonNull(user) && Objects.nonNull(role)) {
            user.getRoles().add(role);
            user.persist();
        }
    }

    @Transactional
    public void removeRoleFromUser(Long userId, String roleName) {
        UserEntity user = UserEntity.findById(userId);
        RoleEntity role = RoleEntity.findByName(roleName);

        if (Objects.nonNull(user) && Objects.nonNull(role)) {
            user.getRoles().remove(role);
            user.persist();
        }
    }

    @Transactional
    public void addPermissionToRole(Long roleId, String permissionName) {
        RoleEntity role = RoleEntity.findById(roleId);
        PermissionEntity permission = PermissionEntity.findByName(permissionName);

        if (Objects.nonNull(role) && Objects.nonNull(permission)) {
            role.getPermissions().add(permission);
            role.persist();
        }
    }

    public boolean userHasPermission(Long userId, String permissionName) {
        UserEntity user = UserEntity.findById(userId);
        return user != null && user.hasPermission(permissionName);
    }

    public Set<String> getUserPermissions(Long userId) {
        UserEntity user = UserEntity.findById(userId);
        return user != null ? user.getAllPermissions() : Set.of();
    }

}
