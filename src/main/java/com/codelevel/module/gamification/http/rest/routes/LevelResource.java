package com.codelevel.module.gamification.http.rest.routes;

import com.codelevel.module.gamification.http.rest.dto.CreateLevelRequest;
import com.codelevel.module.gamification.http.rest.dto.LevelResponse;
import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.module.gamification.persistence.resource.LevelService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/gamification/levels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LevelResource {

    private final LevelService levelService;

    @Inject
    public LevelResource(LevelService levelService) {
        this.levelService = levelService;
    }

    @POST
    @RolesAllowed("ROLE_ADMIN")
    public Response create(CreateLevelRequest request) {
        LevelEntity entity = levelService.create(request.name(), request.xpRequired(), request.badgeIconUrl());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response list() {
        List<LevelResponse> list = levelService.list().stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    private LevelResponse toResponse(LevelEntity e) {
        return new LevelResponse(e.getId(), e.getName(), e.getXpRequired(), e.getBadgeIconUrl());
    }
}
