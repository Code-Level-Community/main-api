package com.codelevel.module.course.http.rest.routes;

import com.codelevel.module.course.http.rest.dto.*;
import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.resource.CourseService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/course")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CourseResource {

    private final CourseService courseService;
    private final JsonWebToken jwt;

    @Inject
    public CourseResource(CourseService courseService, JsonWebToken jwt) {
        this.courseService = courseService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(CourseCreateRequest request) {
        UUID instructorId = extractUserId();
        CourseEntity entity = courseService.create(request, instructorId);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listAll() {
        List<CourseResponse> list = courseService.listAll().stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        CourseEntity entity = courseService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response update(@PathParam("id") Long id, CourseUpdateRequest request) {
        CourseEntity entity = courseService.update(id, request, extractUserId(), isAdmin());
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        courseService.delete(id, extractUserId(), isAdmin());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/publish")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response publish(@PathParam("id") Long id) {
        CourseEntity entity = courseService.publish(id, extractUserId(), isAdmin());
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/archive")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response archive(@PathParam("id") Long id) {
        CourseEntity entity = courseService.archive(id);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/try-approve")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response tryApprove(@PathParam("id") Long id, TryApproveRequest request) {
        courseService.tryApprove(id, request.positiveCount(), request.totalCount());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/category/{categoryId}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response addCategory(@PathParam("id") Long id, @PathParam("categoryId") Long categoryId) {
        courseService.addCategory(id, categoryId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/category/{categoryId}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response removeCategory(@PathParam("id") Long id, @PathParam("categoryId") Long categoryId) {
        courseService.removeCategory(id, categoryId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/tag/{tagId}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response addTag(@PathParam("id") Long id, @PathParam("tagId") Long tagId) {
        courseService.addTag(id, tagId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/tag/{tagId}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response removeTag(@PathParam("id") Long id, @PathParam("tagId") Long tagId) {
        courseService.removeTag(id, tagId);
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("ROLE_ADMIN");
    }

    private CourseResponse toResponse(CourseEntity e) {
        return new CourseResponse(
                e.getId(),
                e.getInstructorId(),
                e.getTitle(),
                e.getDescription(),
                e.getThumbnailUrl(),
                e.getDifficultyLevel() != null ? e.getDifficultyLevel().name() : null,
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getTotalDurationMinutes(),
                e.getTotalLessons(),
                e.getTotalEnrollments(),
                e.getAverageRating(),
                e.getPublishedAt(),
                e.getCreatedAt()
        );
    }
}
