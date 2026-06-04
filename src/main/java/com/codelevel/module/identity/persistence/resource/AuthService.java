package com.codelevel.module.identity.persistence.resource;

import com.codelevel.module.identity.domain.EmailAddress;
import com.codelevel.module.identity.domain.Password;
import com.codelevel.module.identity.domain.PasswordHasher;
import com.codelevel.module.identity.domain.Username;
import com.codelevel.module.identity.http.rest.dto.LoginResponse;
import com.codelevel.module.identity.http.rest.dto.RefreshRequest;
import com.codelevel.module.identity.http.rest.dto.UserPutRequest;
import com.codelevel.module.identity.domain.User;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import com.codelevel.module.identity.persistence.resource.dto.UserSave;
import com.codelevel.module.identity.persistence.entity.RefreshTokenEntity;
import com.codelevel.shared.exception.ApplicationException;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AuthService implements CreateUpdate {

    private static final Logger log = Logger.getLogger(AuthService.class);

    private final TokenService tokenService;
    private final PasswordHasher passwordHasher;

    @Inject
    public AuthService(TokenService tokenService, PasswordHasher passwordHasher) {
        this.tokenService = tokenService;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public LoginResponse login(final String username, final String password) {
        UserEntity entity = (UserEntity) UserEntity.find("lower(username) = lower(?1) and enabled = ?2", username, true)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessRuleException("Invalid Credentials"));

        var user = new User(entity.getPublicId(), entity.getUsername(), entity.getEmail(), entity.getPassword());
        if (!user.matchPass(password, passwordHasher)) throw new BusinessRuleException("Invalid Credentials");

        String accessToken = tokenService.generateAccessToken(user.getPublicId().toString(), user.getUsername(), entity.getRolesAsString());
        String refreshTokenStr = tokenService.generateRefreshToken();
        RefreshTokenEntity.revokeByUsername(user.getUsername());

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setToken(refreshTokenStr);
        refreshTokenEntity.setUsername(user.getUsername());
        refreshTokenEntity.setCreatedAt(LocalDateTime.now());
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        saveOrUpdate(refreshTokenEntity);

        log.infof("User login: username=%s", username);
        return new LoginResponse(accessToken, refreshTokenStr, Duration.ofMinutes(15).getSeconds());
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.findByToken(request.refreshToken())
                .filter(RefreshTokenEntity::isValid)
                .orElseThrow(() -> new ApplicationException("Invalid or expired refresh token"));

        UserEntity entity = UserEntity.findByUsername(refreshTokenEntity.getUsername())
                .orElseThrow(() -> new ResourceNotFound("User not found"));

        String accessToken = tokenService.generateAccessToken(entity.getPublicId().toString(), entity.getUsername(), entity.getRolesAsString());

        log.infof("Token refreshed: username=%s", refreshTokenEntity.getUsername());
        return new LoginResponse(accessToken, refreshTokenEntity.getToken(), Duration.ofMinutes(15).getSeconds());
    }

    @Transactional
    public void logout(UUID publicId) {
        UserEntity.find("publicId = ?1 and enabled = ?2", publicId, true)
                .<UserEntity>firstResultOptional()
                .ifPresent(entity -> {
                    RefreshTokenEntity.revokeByUsername(entity.getUsername());
                    log.infof("User logout: publicId=%s username=%s", publicId, entity.getUsername());
                });
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "user-list-cache")
    public User saveOrUpdate(final UserSave userSave) {
        var user = new User(userSave.name(), userSave.email(), userSave.password());
        var entity = new UserEntity();
        entity.setEmail(user.getEmailAddress());
        entity.setPassword(passwordHasher.hash(user.getPassword()));
        entity.setUsername(user.getUsername());
        entity.setFullName(user.getFullName());
        saveOrUpdate(entity);
        user.setPublicId(entity.getPublicId());
        log.infof("User registered: username=%s", entity.getUsername());
        return user;
    }

    @Transactional
    @CacheInvalidate(cacheName = "user-cache")
    public User update(@CacheKey final UUID id, final UserPutRequest dto) {
        UserEntity entity = UserEntity.find("publicId = ?1 and enabled = ?2", id, true)
                .<UserEntity>firstResultOptional()
                .orElseThrow(() -> new ResourceNotFound("User not found"));
        entity.setEmail(new EmailAddress(dto.email()).value());
        entity.setUsername(new Username(dto.username()).value());
        entity.setPassword(passwordHasher.hash(new Password(dto.password()).value()));
        saveOrUpdate(entity);
        return new User(entity.getPublicId(), entity.getUsername(), entity.getFullName(), entity.getEmail(), entity.getPassword());
    }

    @CacheResult(cacheName = "user-cache")
    public User getUser(@CacheKey final UUID id) {
        return UserEntity.find("publicId = ?1 and enabled = ?2", id, true)
                .<UserEntity>firstResultOptional()
                .map(e -> new User(e.getPublicId(), e.getUsername(), e.getFullName(), e.getEmail(), e.getPassword()))
                .orElseThrow(() -> new ResourceNotFound("User not found"));
    }

    @CacheResult(cacheName = "user-list-cache")
    public Collection<User> getAllUsers() {
        List<UserEntity> entities = UserEntity.find("enabled", true).list();
        return entities.stream()
                .map(entity -> new User(entity.getPublicId(), entity.getUsername(), entity.getFullName(), entity.getEmail(), entity.getPassword()))
                .toList();
    }

    @Transactional
    @CacheInvalidate(cacheName = "user-cache")
    @CacheInvalidateAll(cacheName = "user-list-cache")
    public void delete(@CacheKey final UUID id) {
        UserEntity entity = UserEntity.find("publicId = ?1 and enabled = ?2", id, true)
                .<UserEntity>firstResultOptional()
                .orElseThrow(() -> new ResourceNotFound("User not found"));
        entity.setEnabled(false);
        saveOrUpdate(entity);
        log.infof("User soft-deleted: publicId=%s", id);
    }
}
