package com.codelevel.module.identity.persistence.resource;

import com.codelevel.module.identity.domain.PasswordHasher;
import com.codelevel.module.identity.persistence.entity.PermissionEntity;
import com.codelevel.module.identity.persistence.entity.RoleEntity;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;

@ApplicationScoped
public class UserEntityInitializer {

    private static final Logger log = LoggerFactory.getLogger(UserEntityInitializer.class);

    @Inject
    PasswordHasher passwordHasher;

    @Transactional
    public void createMasterUser(@Observes StartupEvent event) {
        var roles = new HashSet<RoleEntity>();
        if (RoleEntity.count() == 0) {
            log.info("Creating roles");
            var roleAdmin = new RoleEntity();
            roleAdmin.setName("ROLE_ADMIN");
            roleAdmin.setDescription("Administrador do sistema");
            roleAdmin.persist();
            roles.add(roleAdmin);

            var roleUser = new RoleEntity();
            roleUser.setName("ROLE_USER");
            roleUser.setDescription("Usuário comum do sistema");
            roleUser.persist();
            roles.add(roleUser);

            var roleManager = new RoleEntity();
            roleManager.setName("ROLE_INSTRUCTOR");
            roleUser.setDescription("Instrutor de aulas");
            roleManager.persist();
            roles.add(roleManager);
        }

        if (PermissionEntity.count() == 0) {
            log.info("Creating permissions");
            var permissionUserRead = new PermissionEntity();
            permissionUserRead.setName("users.read");
            permissionUserRead.setDescription("Visualizar usuários");
            permissionUserRead.persist();

            var permissionUserWrite = new PermissionEntity();
            permissionUserWrite.setName("users.write");
            permissionUserWrite.setDescription("Criar e editar usuários");
            permissionUserWrite.persist();

            var permissionUserDelete = new PermissionEntity();
            permissionUserDelete.setName("users.delete");
            permissionUserDelete.setDescription("Deletar usuários");
            permissionUserDelete.persist();

            log.info("Created permissions");
        }

        if (UserEntity.count() == 0) {
            log.info("Creating master admin");
            var admin = new UserEntity();
            admin.setUsername("admin");
            admin.setFullName("Admin User");
            admin.setPassword(passwordHasher.hash("admin"));
            admin.setEmail("admin@codelevel.com");
            admin.setRoles(Collections.singleton(roles.stream().filter(role -> role.getName().equals("ROLE_ADMIN")).findFirst().orElse(null)));
            admin.persist();
            log.info("Created master admin");

            var instructor = new UserEntity();
            instructor.setUsername("instructor");
            instructor.setFullName("Instructor User");
            instructor.setPassword(passwordHasher.hash("instructor"));
            instructor.setEmail("instructor@codelevel.com");
            instructor.setRoles(Collections.singleton(roles.stream().filter(role -> role.getName().equals("ROLE_INSTRUCTOR")).findFirst().orElse(null)));
            instructor.persist();
            log.info("Created instructor");

            var user = new UserEntity();
            user.setUsername("user");
            user.setFullName("Regular User");
            user.setPassword(passwordHasher.hash("user"));
            user.setEmail("user@codelevel.com");
            user.setRoles(Collections.singleton(roles.stream().filter(role -> role.getName().equals("ROLE_USER")).findFirst().orElse(null)));
            user.persist();
            log.info("Created user");
        }
    }

}
