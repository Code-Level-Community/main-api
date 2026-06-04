package com.codelevel.module.identity.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    @Column(name = "u_full_name")
    private String fullName;

    @Column(name = "u_avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "u_email", nullable = false, unique = true)
    private String email;

    @Column(name = "u_public_id")
    private UUID publicId;

    @Column(name = "u_last_login")
    private LocalDateTime lastLogin;

    @Column(name = "u_created_at")
    private LocalDateTime createdAt;

    @Column(name = "u_updated_at")
    private LocalDateTime updatedAt;

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
        if (roles.isEmpty()) {
            RoleEntity userRole = RoleEntity.findByName("ROLE_USER");
            if (Objects.nonNull(userRole)) {
                roles.add(userRole);
            }
        }
    }

    public static Optional<UserEntity> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public String getRolesAsString() {
        return roles.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.joining(","));
    }

    public List<String> getRolesAsList() {
        return roles.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

}
