package com.codelevel.module.identity.http.rest.handler;

import com.codelevel.module.identity.persistence.resource.exception.ResourceNotFound;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<ResourceNotFound> {
    @Override
    public Response toResponse(ResourceNotFound exception) {
        int status = 422;
        ErrorResponse error = new ErrorResponse(status, exception.getMessage());
        return Response.status(status)
                .entity(error)
                .build();
    }
}
