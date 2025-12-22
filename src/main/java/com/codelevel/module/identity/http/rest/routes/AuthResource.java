package com.codelevel.module.identity.http.rest.routes;

import com.codelevel.module.identity.domain.exception.BusinessRuleException;
import com.codelevel.module.identity.http.rest.dto.*;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import com.codelevel.module.identity.persistence.resource.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    @ConfigProperty(name = "quarkus.profile")
    String profile;

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(LoginRequest loginData) {
        try {
            LoginResponse response = authService.login(loginData.username(), loginData.password());
            return Response.ok(response).build();
        } catch (BusinessRuleException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refresh(RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN", "ROLE_INSTRUCTOR"})
    public Response getCurrentUser() {
        String username = jwt.getName();
        UserEntity user = UserEntity.findByUsername(username);

        return Response.ok(Map.of(
                "success", true,
                "user", Map.of(
                        "id", user.getPublicId().toString(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "roles", user.getRolesAsString()
                )
        )).build();
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response logout() {
        authService.logout(jwt.getName());

        Response.ResponseBuilder builder = Response.ok("Logout realizado com sucesso");

        if ("prod".equals(profile)) {
            builder.header("Clear-Site-Data", "\"cache\", \"cookies\", \"storage\"");
        }

        return builder.build();
    }



}
