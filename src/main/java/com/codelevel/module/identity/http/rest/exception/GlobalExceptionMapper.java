// java
package com.codelevel.module.identity.http.rest.exception;

import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof BusinessRuleException) {
            return Response.status(422)
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
        }
        if (exception instanceof ResourceNotFound) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
        }
        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            return Response.status(wae.getResponse().getStatus())
                .entity(wae.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Internal server error")
            .type(MediaType.TEXT_PLAIN)
            .build();
    }
}
