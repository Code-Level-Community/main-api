package com.codelevel.module.identity.http.rest.handler;

import com.codelevel.module.identity.persistence.resource.exception.InvalidCredentials;
import com.codelevel.module.identity.persistence.resource.exception.ResourceNotFound;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidCredentialsExceptionMapper implements ExceptionMapper<InvalidCredentials> {

    @Override
    public Response toResponse(InvalidCredentials exception) {
        int status = Response.Status.UNAUTHORIZED.getStatusCode();
        ErrorResponse error = new ErrorResponse(status, exception.getMessage());
        return Response.status(status)
                .entity(error)
                .build();
    }

}
