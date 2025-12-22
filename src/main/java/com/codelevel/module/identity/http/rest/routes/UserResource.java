package com.codelevel.module.identity.http.rest.routes;

import com.codelevel.module.identity.domain.User;
import com.codelevel.module.identity.http.rest.dto.UserPutRequest;
import com.codelevel.module.identity.http.rest.dto.UserSaveRequest;
import com.codelevel.module.identity.http.rest.dto.UserSavedResponse;
import com.codelevel.module.identity.persistence.resource.AuthService;
import com.codelevel.module.identity.persistence.resource.dto.UserSave;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.UUID;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    AuthService authService;

    @POST
    public Response save(UserSaveRequest userSaveRequest) {
        User response = authService.saveOrUpdate(new UserSave(userSaveRequest.username(), userSaveRequest.email(), userSaveRequest.password()));
        var dto = new UserSavedResponse(response.getPublicId(), response.getUsername(), response.getEmail());
        return Response.status(Response.Status.CREATED)
                .entity(dto)
                .build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER"})
    public Response get(@PathParam("id") UUID id) {
        User response = authService.getUser(id);
        var dto = new UserSavedResponse(response.getPublicId(), response.getUsername(), response.getEmail());
        return Response.status(Response.Status.OK)
                .entity(dto)
                .build();
    }

    @GET
    @PermitAll
    public Response getAll() {
        Collection<User> users = authService.getAllUsers();
        var listUsers = users.stream()
                .map(user -> new UserSavedResponse(user.getPublicId(), user.getUsername(), user.getEmail()))
                .toList();
        return Response.status(Response.Status.OK)
                .entity(listUsers)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, UserPutRequest user) {
        User userUpdated = authService.update(id, user);
        var objToReturn = new UserSavedResponse(userUpdated.getPublicId(), userUpdated.getUsername(), userUpdated.getEmail());
        return Response.status(Response.Status.OK)
                .entity(objToReturn)
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        authService.delete(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
