package com.codelevel.module.course.http.rest.routes;

import com.codelevel.module.course.http.rest.dto.TagCreateRequest;
import com.codelevel.module.course.http.rest.dto.TagResponse;
import com.codelevel.module.course.persistence.entity.CourseTagEntity;
import com.codelevel.module.course.persistence.resource.TagService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/tag")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagResource {

    private final TagService tagService;

    @Inject
    public TagResource(TagService tagService) {
        this.tagService = tagService;
    }

    @POST
    @RolesAllowed({"ROLE_ADMIN"})
    public Response create(TagCreateRequest request) {
        CourseTagEntity entity = tagService.create(request);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listAll() {
        List<TagResponse> list = tagService.listAll().stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        CourseTagEntity entity = tagService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    private TagResponse toResponse(CourseTagEntity e) {
        return new TagResponse(e.getId(), e.getName(), e.getSlug(), e.getCreatedAt());
    }
}