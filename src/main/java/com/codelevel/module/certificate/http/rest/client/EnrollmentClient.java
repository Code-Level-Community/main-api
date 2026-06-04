package com.codelevel.module.certificate.http.rest.client;

import com.codelevel.module.certificate.http.rest.client.dto.EnrollmentCompletionDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "student-progress-api")
@Path("/enrollments")
public interface EnrollmentClient {

    @GET
    @Path("/by-user-and-course")
    EnrollmentCompletionDto getByUserAndCourse(
            @QueryParam("userId") UUID userId,
            @QueryParam("courseId") Long courseId
    );
}
