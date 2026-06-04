package com.codelevel.module.student_progress.http.rest.routes;

import com.codelevel.module.student_progress.http.rest.dto.LearningPathCourseAddRequest;
import com.codelevel.module.student_progress.http.rest.dto.LearningPathCourseResponse;
import com.codelevel.module.student_progress.http.rest.dto.LearningPathCreateRequest;
import com.codelevel.module.student_progress.http.rest.dto.LearningPathResponse;
import com.codelevel.module.student_progress.persistence.entity.LearningPathCourseEntity;
import com.codelevel.module.student_progress.persistence.entity.LearningPathEntity;
import com.codelevel.module.student_progress.persistence.resource.LearningPathService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/learning-paths")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LearningPathResource {

    private final LearningPathService learningPathService;
    private final JsonWebToken jwt;

    @Inject
    public LearningPathResource(LearningPathService learningPathService, JsonWebToken jwt) {
        this.learningPathService = learningPathService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(LearningPathCreateRequest request) {
        LearningPathEntity entity = learningPathService.create(
                request.title(), request.description(), request.thumbnailUrl(),
                request.difficultyLevel(), request.prerequisite(), extractUserId());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listPublished() {
        List<LearningPathResponse> list = learningPathService.listPublished()
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response listMy() {
        List<LearningPathResponse> list = learningPathService.listByCreator(extractUserId())
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        LearningPathEntity entity = learningPathService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/publish")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response publish(@PathParam("id") Long id) {
        LearningPathEntity entity = learningPathService.publish(id, extractUserId(), isAdmin());
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/course")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response addCourse(@PathParam("id") Long id, LearningPathCourseAddRequest request) {
        LearningPathCourseEntity entity = learningPathService.addCourse(
                id, request.courseId(), request.orderPosition(), request.learningObjectives(),
                extractUserId(), isAdmin());
        return Response.status(Response.Status.CREATED).entity(toCourseResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}/course/{courseId}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response removeCourse(@PathParam("id") Long id, @PathParam("courseId") Long courseId) {
        learningPathService.removeCourse(id, courseId, extractUserId(), isAdmin());
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/courses")
    @PermitAll
    public Response listCourses(@PathParam("id") Long id) {
        List<LearningPathCourseResponse> list = learningPathService.listCourses(id)
                .stream().map(this::toCourseResponse).toList();
        return Response.ok(list).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        learningPathService.delete(id, extractUserId(), isAdmin());
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("ROLE_ADMIN");
    }

    private LearningPathResponse toResponse(LearningPathEntity e) {
        return new LearningPathResponse(
                e.getId(),
                e.getCreatorId(),
                e.getTitle(),
                e.getDescription(),
                e.getThumbnailUrl(),
                e.getDifficultyLevel() != null ? e.getDifficultyLevel().name() : null,
                e.getPrerequisite(),
                e.getCoursesCount(),
                e.getTotalDurationHours(),
                e.isPublished(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private LearningPathCourseResponse toCourseResponse(LearningPathCourseEntity e) {
        return new LearningPathCourseResponse(
                e.getId(),
                e.getLearningPathId(),
                e.getCourseId(),
                e.getOrderPosition(),
                e.getLearningObjectives(),
                e.getAddedAt()
        );
    }
}
