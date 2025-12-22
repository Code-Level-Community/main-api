package com.codelevel.module.identity.http.rest.handler;

import com.codelevel.module.identity.persistence.resource.exception.ResourceAlreadyExists;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ResourceAlreadyExistExceptionMapper implements ExceptionMapper<ResourceAlreadyExists> {

    @Override
    public Response toResponse(ResourceAlreadyExists exception) {
        int status = Response.Status.CONFLICT.getStatusCode();
        ErrorResponse error = new ErrorResponse(status, exception.getMessage());
        return Response.status(status)
                .entity(error)
                .build();
    }

}
