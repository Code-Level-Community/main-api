package com.codelevel.module.identity.http.rest.handler;

import com.codelevel.shared.exception.BusinessRuleException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<BusinessRuleException> {
    @Override
    public Response toResponse(BusinessRuleException exception) {
        int status = 422;
        ErrorResponse error = new ErrorResponse(status, exception.getMessage());
        return Response.status(status)
                .entity(error)
                .build();
    }
}
