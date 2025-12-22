package com.codelevel.module.identity.persistence.resource;

import com.codelevel.module.identity.http.rest.dto.LoginResponse;
import com.codelevel.module.identity.http.rest.dto.RefreshRequest;
import com.codelevel.module.identity.http.rest.dto.UserPutRequest;
import com.codelevel.module.identity.domain.User;
import com.codelevel.module.identity.domain.exception.BusinessRuleException;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import com.codelevel.module.identity.persistence.resource.dto.UserSave;
import com.codelevel.module.identity.persistence.resource.exception.ApplicationException;
import com.codelevel.module.identity.persistence.resource.exception.ResourceAlreadyExists;
import com.codelevel.module.identity.persistence.resource.exception.ResourceNotFound;
import com.codelevel.module.identity.persistence.entity.RefreshTokenEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class AuthService implements CreateUpdate {

    @Inject
    TokenService tokenService;

    @Transactional
    public LoginResponse login(final String username, final String password) {
        UserEntity entity = UserEntity.find("username = ?1 and enabled = ?2", username, true).firstResult();
        if (Objects.isNull(entity)) throw new ResourceNotFound("Invalid Credentials");
        var user = new User(entity.getUsername(), entity.getEmail(), entity.getPassword());
        if (!user.matchPass(password)) throw new BusinessRuleException("Invalid Credentials");

        String accessToken = tokenService.generateAccessToken(user.getUsername(), entity.getRolesAsString());
        String refreshTokenStr = tokenService.generateRefreshToken();
        RefreshTokenEntity.revokeByUsername(user.getUsername());
        // Salva novo refresh token
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setToken(refreshTokenStr);
        refreshTokenEntity.setUsername(user.getUsername());
        refreshTokenEntity.setCreatedAt(LocalDateTime.now());
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        saveOrUpdate(refreshTokenEntity);

        return new LoginResponse(accessToken, refreshTokenStr, Duration.ofMinutes(15).getSeconds());
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.findByToken(request.refreshToken());

        if (Objects.isNull(refreshTokenEntity) || !refreshTokenEntity.isValid()) {
            throw new ApplicationException("Refresh token inválido ou expirado");
        }

        UserEntity entity = UserEntity.findByUsername(refreshTokenEntity.getUsername());
        if (Objects.isNull(entity)) throw new ResourceNotFound("User not found");

        String accessToken = tokenService.generateAccessToken(entity.getUsername(), entity.getRolesAsString());

        return new LoginResponse(accessToken, refreshTokenEntity.getToken(), Duration.ofMinutes(15).getSeconds());
    }

    @Transactional
    public void logout(String username) {
        RefreshTokenEntity.revokeByUsername(username);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "user-list-cache")
    public User saveOrUpdate(final UserSave userSave) {
        var user = new User(userSave.name(), userSave.email(), userSave.password());
        var entity = new UserEntity();
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setUsername(user.getUsername());
        saveOrUpdate(entity);
        user.setPublicId(entity.getPublicId());
        return user;
    }

    @CacheInvalidate(cacheName = "user-cache")
    public User update(@CacheKey final UUID id, final UserPutRequest dto) {
        User user = new User(dto.username(), dto.email(), dto.password());
        UserEntity entity = UserEntity.find("id = ?1 and enabled = ?2", id, true).firstResult();
        if (Objects.isNull(entity)) throw new ResourceNotFound("User not found");
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setUsername(user.getUsername());
        saveOrUpdate(entity);
        return user;
    }

    @CacheResult(cacheName = "user-cache")
    public User getUser(@CacheKey final UUID id) {
        UserEntity entity = UserEntity.find("publicId = ?1 and enabled = ?2", id, true).firstResult();
        if (Objects.isNull(entity)) throw new ResourceNotFound("User not found");
        return new User(entity.getPublicId(), entity.getUsername(), entity.getEmail(), entity.getPassword());
    }

    @CacheResult(cacheName = "user-list-cache")
    public Collection<User> getAllUsers() {
        List<UserEntity> entities = UserEntity.find("enabled", true)
                .list();
        return entities
                .stream()
                .map(entity -> new User(entity.getPublicId(), entity.getUsername(), entity.getEmail(), entity.getPassword()))
                .toList();
    }

    @Transactional
    @CacheInvalidate(cacheName = "user-cache")
    @CacheInvalidateAll(cacheName = "user-list-cache")
    public void delete(@CacheKey final UUID id) {
        UserEntity entity = UserEntity.find("publicId = ?1 and enabled = ?2", id, true).firstResult();
        if (Objects.isNull(entity)) throw new ResourceNotFound("User not found");
        entity.setEnabled(false);
        saveOrUpdate(entity);
    }

}
