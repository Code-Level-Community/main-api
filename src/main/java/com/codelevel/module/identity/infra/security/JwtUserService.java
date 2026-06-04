// java
package com.codelevel.module.identity.infra.security;

import com.codelevel.module.identity.persistence.entity.UserEntity;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class JwtUserService {

    @Inject
    public JwtUserService() { }

    /**
     * Resolve user by 'sub' claim (espera UUID). Lança WebApplicationException(401) se faltar/for inválido.
     * Lança ResourceNotFound se usuário não existir.
     */
    public UserEntity resolveUserFromJwt(JsonWebToken jwt) {
        Object sub = jwt.getClaim("sub");
        if (Objects.isNull(sub)) {
            throw new jakarta.ws.rs.WebApplicationException("Missing subject claim", jakarta.ws.rs.core.Response.Status.UNAUTHORIZED);
        }

        UUID publicId;
        try {
            publicId = UUID.fromString(sub.toString());
        } catch (IllegalArgumentException e) {
            throw new jakarta.ws.rs.WebApplicationException("Invalid subject claim", jakarta.ws.rs.core.Response.Status.UNAUTHORIZED);
        }

        UserEntity user = UserEntity.find("publicId = ?1 and enabled = true", publicId).firstResult();
        if (Objects.isNull(user)) {
            throw new ResourceNotFound("User not found");
        }

        return user;
    }
}
