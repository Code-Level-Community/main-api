package com.codelevel.module.reviews_feedbacks.http.rest.routes;

import com.codelevel.module.reviews_feedbacks.http.rest.dto.CourseReviewCreateRequest;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.CourseReviewResponse;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.CourseReviewUpdateRequest;
import com.codelevel.module.reviews_feedbacks.persistence.entity.CourseReviewEntity;
import com.codelevel.module.reviews_feedbacks.persistence.resource.CourseReviewService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/course-review")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CourseReviewResource {

    private final CourseReviewService courseReviewService;
    private final JsonWebToken jwt;

    @Inject
    public CourseReviewResource(CourseReviewService courseReviewService, JsonWebToken jwt) {
        this.courseReviewService = courseReviewService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(CourseReviewCreateRequest request) {
        UUID userId = extractUserId();
        CourseReviewEntity entity = courseReviewService.create(request, userId);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        CourseReviewEntity entity = courseReviewService.findById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @GET
    @Path("/by-course/{courseId}")
    @PermitAll
    public Response getByCourse(@PathParam("courseId") Long courseId) {
        List<CourseReviewResponse> list = courseReviewService.findByCourse(courseId)
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/by-user")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getByUser() {
        UUID userId = extractUserId();
        List<CourseReviewResponse> list = courseReviewService.findByUser(userId)
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response update(@PathParam("id") Long id, CourseReviewUpdateRequest request) {
        UUID userId = extractUserId();
        CourseReviewEntity entity = courseReviewService.update(id, request, userId);
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        UUID userId = extractUserId();
        courseReviewService.delete(id, userId, isAdmin());
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("ROLE_ADMIN");
    }

    private CourseReviewResponse toResponse(CourseReviewEntity e) {
        return new CourseReviewResponse(
                e.getId(),
                e.getUserId(),
                e.getCourseId(),
                e.getRating(),
                e.isPositive(),
                e.getComment(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
