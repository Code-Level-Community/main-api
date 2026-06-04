package com.codelevel.module.course_requests.http.rest.routes;

import com.codelevel.module.course_requests.http.rest.dto.CompleteRequest;
import com.codelevel.module.course_requests.http.rest.dto.CourseRequestCreateRequest;
import com.codelevel.module.course_requests.http.rest.dto.CourseRequestResponse;
import com.codelevel.module.course_requests.http.rest.dto.VoteRequest;
import com.codelevel.module.course_requests.persistence.entity.CourseRequestEntity;
import com.codelevel.module.course_requests.persistence.entity.enums.CourseRequestVoteType;
import com.codelevel.module.course_requests.persistence.entity.enums.StatusCourseRequest;
import com.codelevel.module.course_requests.persistence.resource.CourseRequestService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/course-request")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CourseRequestResource {

    private final CourseRequestService courseRequestService;
    private final JsonWebToken jwt;

    @Inject
    public CourseRequestResource(CourseRequestService courseRequestService, JsonWebToken jwt) {
        this.courseRequestService = courseRequestService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(CourseRequestCreateRequest request) {
        CourseRequestEntity entity = courseRequestService.create(request, extractUserId());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listAll(@QueryParam("status") String status) {
        List<CourseRequestResponse> list;
        if (status != null && !status.isBlank()) {
            list = courseRequestService.listByStatus(StatusCourseRequest.valueOf(status))
                    .stream().map(this::toResponse).toList();
        } else {
            list = courseRequestService.listAll().stream().map(this::toResponse).toList();
        }
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        CourseRequestEntity entity = courseRequestService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/vote")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response vote(@PathParam("id") Long id, VoteRequest request) {
        CourseRequestVoteType voteType = CourseRequestVoteType.valueOf(request.voteType());
        CourseRequestEntity entity = courseRequestService.vote(id, extractUserId(), voteType);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/reject")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response reject(@PathParam("id") Long id) {
        CourseRequestEntity entity = courseRequestService.reject(id);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/accept")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response accept(@PathParam("id") Long id) {
        CourseRequestEntity entity = courseRequestService.accept(id, extractUserId());
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/give-up")
    @RolesAllowed({"ROLE_INSTRUCTOR"})
    public Response giveUp(@PathParam("id") Long id) {
        CourseRequestEntity entity = courseRequestService.giveUp(id, extractUserId());
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/complete")
    @RolesAllowed({"ROLE_INSTRUCTOR"})
    public Response complete(@PathParam("id") Long id, CompleteRequest request) {
        CourseRequestEntity entity = courseRequestService.complete(id, extractUserId(), request.createdCourseId());
        return Response.ok(toResponse(entity)).build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private CourseRequestResponse toResponse(CourseRequestEntity e) {
        return new CourseRequestResponse(
                e.getId(),
                e.getRequesterId(),
                e.getTitle(),
                e.getDescription(),
                e.getCategoryId(),
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getUpvotes(),
                e.getDownvotes(),
                e.getApprovalPercentage(),
                e.getAssignedInstructorId(),
                e.getCreatedCourseId(),
                e.getCreatedAt(),
                e.getApprovedAt(),
                e.getCompletedAt()
        );
    }
}
