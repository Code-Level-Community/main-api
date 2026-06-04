package com.codelevel.module.identity.http.rest.routes;

import com.codelevel.module.identity.domain.User;
import com.codelevel.module.identity.http.rest.mapper.UserMapper;
import com.codelevel.module.identity.persistence.resource.dto.UserSave;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.module.identity.http.rest.dto.*;
import com.codelevel.module.identity.persistence.entity.UserEntity;
import com.codelevel.module.identity.persistence.resource.AuthService;
import com.codelevel.module.identity.infra.security.JwtUserService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import static jakarta.ws.rs.core.Response.Status.*;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final String PROD_ENVIRONMENT = "prod";
    private static final Logger log = LoggerFactory.getLogger(AuthResource.class);

    private final String profile;
    private final AuthService authService;
    private final JsonWebToken jwt;
    private final JwtUserService jwtUserService;

    @Inject
    public AuthResource(
            @ConfigProperty(name = "quarkus.profile") String profile,
            AuthService authService,
            JsonWebToken jwt,
            JwtUserService jwtUserService) {
        this.profile = profile;
        this.authService = authService;
        this.jwt = jwt;
        this.jwtUserService = jwtUserService;
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(LoginRequest loginData) {
        try {
            LoginResponse response = authService.login(loginData.username(), loginData.password());
            return Response.ok(response).build();
        } catch (BusinessRuleException e) {
            return Response.status(UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/signup")
    @PermitAll
    public Response signup(UserSaveRequest userSaveDTO) {
        log.info("Data received for user signup: {}", userSaveDTO);
        User user = authService.saveOrUpdate(new UserSave(userSaveDTO.fullName(), userSaveDTO.email(), userSaveDTO.password()));
        var responseDTO = new UserSavedResponse(user.getPublicId(), user.getUsername(), user.getEmailAddress(), user.getFullName());
        return Response.status(CREATED).entity(responseDTO).build();
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
        UserEntity user = jwtUserService.resolveUserFromJwt(jwt);
        UserResponse userDto = UserMapper.toResponse(user);

        return Response.ok(Map.of(
            "success", true,
            "user", userDto
        )).build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout() {
        authService.logout(UUID.fromString(jwt.getSubject()));

        Response.ResponseBuilder builder = Response.ok("Logout successful");

        if (PROD_ENVIRONMENT.equals(profile)) {
            applyClearSiteDataHeader(builder);
        }

        return builder.build();
    }

    private void applyClearSiteDataHeader(Response.ResponseBuilder builder) {
        builder.header("Clear-Site-Data", "\"cache\", \"cookies\", \"storage\"");
    }
}
