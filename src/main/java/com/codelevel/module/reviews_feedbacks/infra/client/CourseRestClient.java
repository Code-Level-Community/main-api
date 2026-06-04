package com.codelevel.module.reviews_feedbacks.infra.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "course-api")
@Produces(MediaType.APPLICATION_JSON)
public interface CourseRestClient {

    @GET
    @Path("/course/{id}")
    Response getCourse(@PathParam("id") Long id);
}
