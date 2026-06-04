package com.codelevel.module.student_progress.http.rest.routes;

import com.codelevel.module.student_progress.http.rest.dto.EnrollmentCreateRequest;
import com.codelevel.module.student_progress.http.rest.dto.EnrollmentResponse;
import com.codelevel.module.student_progress.persistence.entity.CourseEnrollmentEntity;
import com.codelevel.module.student_progress.persistence.resource.EnrollmentService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/enrollments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnrollmentResource {

    private final EnrollmentService enrollmentService;
    private final JsonWebToken jwt;

    @Inject
    public EnrollmentResource(EnrollmentService enrollmentService, JsonWebToken jwt) {
        this.enrollmentService = enrollmentService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response enroll(EnrollmentCreateRequest request) {
        CourseEnrollmentEntity entity = enrollmentService.enroll(request.courseId(), extractUserId());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getById(@PathParam("id") Long id) {
        CourseEnrollmentEntity entity = enrollmentService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response listMy() {
        List<EnrollmentResponse> list = enrollmentService.listByUserId(extractUserId())
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/by-course/{courseId}")
    @PermitAll
    public Response listByCourse(@PathParam("courseId") Long courseId) {
        List<EnrollmentResponse> list = enrollmentService.listByCourseId(courseId)
                .stream().map(this::toResponse).toList();
    return Response.ok(list).build();
    }

    @GET
    @Path("/by-user-and-course")
    @PermitAll
    public Response getByUserAndCourse(@QueryParam("userId") UUID userId, @QueryParam("courseId") Long courseId) {
        EnrollmentResponse response = toResponse(enrollmentService.findByUserAndCourse(userId, courseId));
        return Response.ok(response).build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private EnrollmentResponse toResponse(CourseEnrollmentEntity e) {
        return new EnrollmentResponse(
                e.getId(),
                e.getUserId(),
                e.getCourseId(),
                e.getEnrolledAt(),
                e.getStartedAt(),
                e.getCompletedAt(),
                e.getProgressPercentage(),
                e.getLessonsCompleted(),
                e.getTotalStudyTimeMinutes()
        );
    }
}
