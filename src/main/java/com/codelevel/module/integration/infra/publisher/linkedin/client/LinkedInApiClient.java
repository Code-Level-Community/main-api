package com.codelevel.module.integration.infra.publisher.linkedin.client;

import com.codelevel.module.integration.infra.publisher.linkedin.dto.LinkedInUgcPostRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "linkedin-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface LinkedInApiClient {

    @POST
    @Path("/v2/ugcPosts")
    void createPost(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Restli-Protocol-Version") String restliVersion,
            LinkedInUgcPostRequest request
    );
}