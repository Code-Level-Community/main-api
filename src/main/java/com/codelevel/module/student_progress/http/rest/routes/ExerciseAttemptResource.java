package com.codelevel.module.student_progress.http.rest.routes;

import com.codelevel.module.student_progress.http.rest.dto.ExerciseAttemptRequest;
import com.codelevel.module.student_progress.http.rest.dto.ExerciseAttemptResponse;
import com.codelevel.module.student_progress.persistence.entity.ExerciseAttemptEntity;
import com.codelevel.module.student_progress.persistence.resource.ExerciseAttemptService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/exercise-attempts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExerciseAttemptResource {

    private final ExerciseAttemptService exerciseAttemptService;
    private final JsonWebToken jwt;

    @Inject
    public ExerciseAttemptResource(ExerciseAttemptService exerciseAttemptService, JsonWebToken jwt) {
        this.exerciseAttemptService = exerciseAttemptService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response submit(ExerciseAttemptRequest request) {
        ExerciseAttemptEntity entity = exerciseAttemptService.submit(
                extractUserId(),
                request.exerciseId(),
                request.submittedAnswer(),
                request.correct(),
                request.baseXpReward(),
                request.maxAttempts(),
                request.feedback()
        );
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @Path("/by-exercise/{exerciseId}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response listByExercise(@PathParam("exerciseId") Long exerciseId) {
        List<ExerciseAttemptResponse> list = exerciseAttemptService.listByUserAndExercise(extractUserId(), exerciseId)
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response listMy() {
        List<ExerciseAttemptResponse> list = exerciseAttemptService.listByUserId(extractUserId())
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private ExerciseAttemptResponse toResponse(ExerciseAttemptEntity e) {
        return new ExerciseAttemptResponse(
                e.getId(),
                e.getUserId(),
                e.getExerciseId(),
                e.getAttemptNumber(),
                e.getSubmittedAnswer(),
                e.isCorrect(),
                e.getXpEarned(),
                e.getFeedback(),
                e.getSubmittedAt()
        );
    }
}