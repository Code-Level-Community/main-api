package com.codelevel.module.course.http.rest.routes;

import com.codelevel.module.course.http.rest.dto.LessonCreateRequest;
import com.codelevel.module.course.http.rest.dto.LessonResponse;
import com.codelevel.module.course.http.rest.dto.LessonUpdateRequest;
import com.codelevel.module.course.persistence.entity.LessonEntity;
import com.codelevel.module.course.persistence.resource.LessonService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/module/{moduleId}/lesson")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LessonResource {

    private final LessonService lessonService;

    @Inject
    public LessonResource(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @POST
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(@PathParam("moduleId") Long moduleId, LessonCreateRequest request) {
        LessonEntity entity = lessonService.create(moduleId, request);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listByModule(@PathParam("moduleId") Long moduleId) {
        List<LessonResponse> list = lessonService.listByModule(moduleId).stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("moduleId") Long moduleId, @PathParam("id") Long id) {
        LessonEntity entity = lessonService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response update(@PathParam("moduleId") Long moduleId, @PathParam("id") Long id, LessonUpdateRequest request) {
        LessonEntity entity = lessonService.update(id, request);
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response delete(@PathParam("moduleId") Long moduleId, @PathParam("id") Long id) {
        lessonService.delete(id);
        return Response.noContent().build();
    }

    private LessonResponse toResponse(LessonEntity e) {
        return new LessonResponse(
                e.getId(),
                e.getModuleId(),
                e.getTitle(),
                e.getDescription(),
                e.getContentType() != null ? e.getContentType().name() : null,
                e.getOrderPosition(),
                e.getXpReward(),
                e.isHasExercises(),
                e.getCreatedAt()
        );
    }
}
