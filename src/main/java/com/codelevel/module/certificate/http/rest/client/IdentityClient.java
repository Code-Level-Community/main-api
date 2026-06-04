package com.codelevel.module.certificate.http.rest.client;

import com.codelevel.module.certificate.http.rest.client.dto.UserPublicDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "identity-api")
@Path("/user")
public interface IdentityClient {

    @GET
    @Path("/{id}")
    UserPublicDto getUser(@PathParam("id") UUID id, @HeaderParam("Authorization") String authHeader);
}
