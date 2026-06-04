package com.codelevel.module.integration.infra.publisher.instagram.client;

import com.codelevel.module.integration.infra.publisher.instagram.dto.InstagramContainerResponse;
import com.codelevel.module.integration.infra.publisher.instagram.dto.InstagramPublishResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "instagram-api")
@Produces(MediaType.APPLICATION_JSON)
public interface InstagramApiClient {

    @POST
    @Path("/{userId}/media")
    InstagramContainerResponse createContainer(
            @PathParam("userId") String userId,
            @QueryParam("image_url") String imageUrl,
            @QueryParam("caption") String caption,
            @QueryParam("access_token") String accessToken
    );

    @POST
    @Path("/{userId}/media_publish")
    InstagramPublishResponse publishContainer(
            @PathParam("userId") String userId,
            @QueryParam("creation_id") String creationId,
            @QueryParam("access_token") String accessToken
    );
}