package com.codelevel.module.integration.http.rest.routes;

import com.codelevel.module.integration.http.rest.dto.CreateSocialMediaPostRequest;
import com.codelevel.module.integration.http.rest.dto.SocialMediaPostResponse;
import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPlatform;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostStatus;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostType;
import com.codelevel.module.integration.persistence.resource.SocialMediaPostService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/integration/posts")
@Produces(MediaType.APPLICATION_JSON)
public class SocialMediaPostResource {

    private final SocialMediaPostService service;
    private final JsonWebToken jwt;

    @Inject
    public SocialMediaPostResource(SocialMediaPostService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ROLE_ADMIN")
    public Response create(CreateSocialMediaPostRequest request) {
        SocialMediaPostEntity entity = service.createPost(request, extractUserId());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @RolesAllowed({"ROLE_ADMIN", "ROLE_INSTRUCTOR"})
    public Response listAll(
            @QueryParam("platform") String platform,
            @QueryParam("status") String status,
            @QueryParam("postType") String postType) {

        List<SocialMediaPostResponse> result;

        if (platform != null) {
            result = service.listByPlatform(SocialMediaPlatform.valueOf(platform))
                    .stream().map(this::toResponse).toList();
        } else if (status != null) {
            result = service.listByStatus(SocialMediaPostStatus.valueOf(status))
                    .stream().map(this::toResponse).toList();
        } else if (postType != null) {
            result = service.listByType(SocialMediaPostType.valueOf(postType))
                    .stream().map(this::toResponse).toList();
        } else {
            result = service.listAll().stream().map(this::toResponse).toList();
        }

        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_INSTRUCTOR"})
    public Response getById(@PathParam("id") Long id) {
        return Response.ok(toResponse(service.getById(id))).build();
    }

    @POST
    @Path("/{id}/publish")
    @RolesAllowed("ROLE_ADMIN")
    public Response publish(@PathParam("id") Long id) {
        return Response.ok(toResponse(service.publishPost(id))).build();
    }

    @POST
    @Path("/{id}/retry")
    @RolesAllowed("ROLE_ADMIN")
    public Response retry(@PathParam("id") Long id) {
        return Response.ok(toResponse(service.retryPost(id))).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    public Response cancel(@PathParam("id") Long id) {
        service.cancelPost(id);
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private SocialMediaPostResponse toResponse(SocialMediaPostEntity e) {
        return new SocialMediaPostResponse(
                e.getId(),
                e.getUserId(),
                e.getPlatform().name(),
                e.getPostType().name(),
                e.getReferenceId(),
                e.getContent(),
                e.getMediaUrl(),
                e.getStatus().name(),
                e.getErrorMessage(),
                e.getScheduledAt(),
                e.getPostedAt(),
                e.getCreatedAt()
        );
    }
}