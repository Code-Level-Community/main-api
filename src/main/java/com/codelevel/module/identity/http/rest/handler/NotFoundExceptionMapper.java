package com.codelevel.module.identity.http.rest.handler;

import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<ResourceNotFound> {

    @Override
    public Response toResponse(ResourceNotFound exception) {
        int status = Response.Status.NOT_FOUND.getStatusCode();
        ErrorResponse error = new ErrorResponse(status, exception.getMessage());
        return Response.status(status)
                .entity(error)
                .build();
    }

}
