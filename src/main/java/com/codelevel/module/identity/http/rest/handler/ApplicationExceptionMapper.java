package com.codelevel.module.identity.http.rest.handler;

import com.codelevel.module.identity.persistence.resource.exception.ApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    @Override
    public Response toResponse(ApplicationException exception) {
        int status = Response.Status.BAD_REQUEST.getStatusCode();
        ErrorResponse error = new ErrorResponse(status, exception.getMessage());
        return Response.status(status)
                .entity(error)
                .build();
    }

}
