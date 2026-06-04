package com.codelevel.module.reviews_feedbacks.http.rest.routes;

import com.codelevel.module.reviews_feedbacks.http.rest.dto.LessonFeedbackCreateRequest;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.LessonFeedbackResponse;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.LessonFeedbackUpdateRequest;
import com.codelevel.module.reviews_feedbacks.persistence.entity.LessonFeedbackEntity;
import com.codelevel.module.reviews_feedbacks.persistence.resource.LessonFeedbackService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/lesson-feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LessonFeedbackResource {

    private final LessonFeedbackService lessonFeedbackService;
    private final JsonWebToken jwt;

    @Inject
    public LessonFeedbackResource(LessonFeedbackService lessonFeedbackService, JsonWebToken jwt) {
        this.lessonFeedbackService = lessonFeedbackService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(LessonFeedbackCreateRequest request) {
        UUID userId = extractUserId();
        LessonFeedbackEntity entity = lessonFeedbackService.create(request, userId);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        LessonFeedbackEntity entity = lessonFeedbackService.findById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @GET
    @Path("/by-lesson/{lessonId}")
    @PermitAll
    public Response getByLesson(@PathParam("lessonId") Long lessonId) {
        List<LessonFeedbackResponse> list = lessonFeedbackService.findByLesson(lessonId)
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/by-user")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getByUser() {
        UUID userId = extractUserId();
        List<LessonFeedbackResponse> list = lessonFeedbackService.findByUser(userId)
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response update(@PathParam("id") Long id, LessonFeedbackUpdateRequest request) {
        UUID userId = extractUserId();
        LessonFeedbackEntity entity = lessonFeedbackService.update(id, request, userId);
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        UUID userId = extractUserId();
        lessonFeedbackService.delete(id, userId, isAdmin());
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("ROLE_ADMIN");
    }

    private LessonFeedbackResponse toResponse(LessonFeedbackEntity e) {
        return new LessonFeedbackResponse(
                e.getId(),
                e.getUserId(),
                e.getLessonId(),
                e.isHelpful(),
                e.getComment(),
                e.getCreatedAt()
        );
    }
}
