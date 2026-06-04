package com.codelevel.module.course.http.rest.routes;

import com.codelevel.module.course.http.rest.dto.CategoryCreateRequest;
import com.codelevel.module.course.http.rest.dto.CategoryResponse;
import com.codelevel.module.course.persistence.entity.CourseCategoryEntity;
import com.codelevel.module.course.persistence.resource.CategoryService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/category")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private final CategoryService categoryService;

    @Inject
    public CategoryResource(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @POST
    @RolesAllowed({"ROLE_ADMIN"})
    public Response create(CategoryCreateRequest request) {
        CourseCategoryEntity entity = categoryService.create(request);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listAll() {
        List<CategoryResponse> list = categoryService.listAll().stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        CourseCategoryEntity entity = categoryService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    private CategoryResponse toResponse(CourseCategoryEntity e) {
        return new CategoryResponse(e.getId(), e.getName(), e.getSlug(), e.getIconUrl(), e.getCreatedAt());
    }
}