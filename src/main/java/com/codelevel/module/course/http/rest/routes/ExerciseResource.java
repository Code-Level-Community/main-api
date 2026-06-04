package com.codelevel.module.course.http.rest.routes;

import com.codelevel.module.course.http.rest.dto.ExerciseCreateRequest;
import com.codelevel.module.course.http.rest.dto.ExerciseResponse;
import com.codelevel.module.course.http.rest.dto.ExerciseUpdateRequest;
import com.codelevel.module.course.persistence.entity.ExerciseEntity;
import com.codelevel.module.course.persistence.resource.ExerciseService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/lesson/{lessonId}/exercise")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExerciseResource {

    private final ExerciseService exerciseService;

    @Inject
    public ExerciseResource(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @POST
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(@PathParam("lessonId") Long lessonId, ExerciseCreateRequest request) {
        ExerciseEntity entity = exerciseService.create(lessonId, request);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listByLesson(@PathParam("lessonId") Long lessonId) {
        List<ExerciseResponse> list = exerciseService.listByLesson(lessonId).stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("lessonId") Long lessonId, @PathParam("id") Long id) {
        ExerciseEntity entity = exerciseService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response update(@PathParam("lessonId") Long lessonId, @PathParam("id") Long id, ExerciseUpdateRequest request) {
        ExerciseEntity entity = exerciseService.update(id, request);
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response delete(@PathParam("lessonId") Long lessonId, @PathParam("id") Long id) {
        exerciseService.delete(id);
        return Response.noContent().build();
    }

    private ExerciseResponse toResponse(ExerciseEntity e) {
        return new ExerciseResponse(
                e.getId(),
                e.getLessonId(),
                e.getType() != null ? e.getType().name() : null,
                e.getTitle(),
                e.getDescription(),
                e.getMaxAttempts(),
                e.getXpReward(),
                e.getCreatedAt()
        );
    }
}
