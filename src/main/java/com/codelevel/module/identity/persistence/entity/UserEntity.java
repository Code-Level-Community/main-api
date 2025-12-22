package com.codelevel.module.identity.persistence.entity;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table(name="CL_USER")
@Entity
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_user_seq", allocationSize = 1)
    private Long id;

    @Column(name = "u_username", nullable = false, unique = true)
    private String username;

    @Column(name = "u_pass", nullable = false)
    private String password;

    @Column(name = "u_email", nullable = false, unique = true)
    private String email;

    @Column(name = "u_public_id")
    private UUID publicId;

    @Column(name = "u_enabled")
    private Boolean enabled;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "CL_USER_ROLE",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (Objects.isNull(publicId)) publicId = UUID.randomUUID();
        if (Objects.isNull(enabled)) enabled = true;
        this.password = BcryptUtil.bcryptHash(password, 10);
        if (roles.isEmpty()) {
            RoleEntity userRole = RoleEntity.findByName("ROLE_USER");
            if (Objects.nonNull(userRole)) {
                roles.add(userRole);
            }
        }
    }

    public static UserEntity findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public String getRolesAsString() {
        return roles.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.joining(","));
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    public Set<String> getAllPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }

    public boolean hasPermission(String permissionName) {
        return getAllPermissions().contains(permissionName);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

}
