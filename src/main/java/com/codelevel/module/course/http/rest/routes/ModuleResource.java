package com.codelevel.module.course.http.rest.routes;

import com.codelevel.module.course.http.rest.dto.ModuleCreateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleResponse;
import com.codelevel.module.course.http.rest.dto.ModuleUpdateRequest;
import com.codelevel.module.course.persistence.entity.ModuleEntity;
import com.codelevel.module.course.persistence.resource.ModuleService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/course/{courseId}/module")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModuleResource {

    private final ModuleService moduleService;

    @Inject
    public ModuleResource(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @POST
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(@PathParam("courseId") Long courseId, ModuleCreateRequest request) {
        ModuleEntity entity = moduleService.create(courseId, request);
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listByCourse(@PathParam("courseId") Long courseId) {
        List<ModuleResponse> list = moduleService.listByCourse(courseId).stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("courseId") Long courseId, @PathParam("id") Long id) {
        ModuleEntity entity = moduleService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response update(@PathParam("courseId") Long courseId, @PathParam("id") Long id, ModuleUpdateRequest request) {
        ModuleEntity entity = moduleService.update(id, request);
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response delete(@PathParam("courseId") Long courseId, @PathParam("id") Long id) {
        moduleService.delete(id);
        return Response.noContent().build();
    }

    private ModuleResponse toResponse(ModuleEntity e) {
        return new ModuleResponse(e.getId(), e.getCourseId(), e.getTitle(), e.getDescription(), e.getOrderPosition(), e.getCreatedAt());
    }
}