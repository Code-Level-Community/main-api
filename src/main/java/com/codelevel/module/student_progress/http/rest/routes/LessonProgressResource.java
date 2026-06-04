package com.codelevel.module.student_progress.http.rest.routes;

import com.codelevel.module.student_progress.http.rest.dto.LessonProgressRequest;
import com.codelevel.module.student_progress.http.rest.dto.LessonProgressResponse;
import com.codelevel.module.student_progress.persistence.entity.LessonProgressEntity;
import com.codelevel.module.student_progress.persistence.resource.LessonProgressService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/lesson-progress")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LessonProgressResource {

    private final LessonProgressService lessonProgressService;
    private final JsonWebToken jwt;

    @Inject
    public LessonProgressResource(LessonProgressService lessonProgressService, JsonWebToken jwt) {
        this.lessonProgressService = lessonProgressService;
        this.jwt = jwt;
    }

    @POST
    @Path("/track")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response track(LessonProgressRequest request) {
        LessonProgressEntity entity = lessonProgressService.trackProgress(
                extractUserId(), request.lessonId(), request.watchTimeSeconds(), request.videoDurationSeconds());
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{lessonId}/complete")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response markComplete(@PathParam("lessonId") Long lessonId) {
        LessonProgressEntity entity = lessonProgressService.markComplete(extractUserId(), lessonId);
        return Response.ok(toResponse(entity)).build();
    }

    @GET
    @Path("/{lessonId}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getByLesson(@PathParam("lessonId") Long lessonId) {
        LessonProgressEntity entity = lessonProgressService.getByUserAndLesson(extractUserId(), lessonId);
        return Response.ok(toResponse(entity)).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response listMy() {
        List<LessonProgressResponse> list = lessonProgressService.listByUserId(extractUserId())
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private LessonProgressResponse toResponse(LessonProgressEntity e) {
        return new LessonProgressResponse(
                e.getId(),
                e.getUserId(),
                e.getLessonId(),
                e.isCompleted(),
                e.getWatchTimeSeconds(),
                e.getVideoDurationSeconds(),
                e.getCompletionPercentage(),
                e.getStartedAt(),
                e.getCompletedAt(),
                e.getLastWatchedAt()
        );
    }
}
