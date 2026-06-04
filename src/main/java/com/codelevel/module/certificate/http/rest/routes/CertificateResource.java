package com.codelevel.module.certificate.http.rest.routes;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.http.rest.client.EnrollmentClient;
import com.codelevel.module.certificate.http.rest.client.IdentityClient;
import com.codelevel.module.certificate.http.rest.client.dto.EnrollmentCompletionDto;
import com.codelevel.module.certificate.http.rest.client.dto.UserPublicDto;
import com.codelevel.module.certificate.http.rest.dto.CertificateRequestBody;
import com.codelevel.module.certificate.http.rest.dto.CertificateResponse;
import com.codelevel.module.certificate.http.rest.mapper.CertificateMapper;
import com.codelevel.module.certificate.persistence.resource.CertificateService;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.UUID;

@Path("/certificates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificateResource {

    private final CertificateService certificateService;
    private final CertificateMapper mapper;
    private final JsonWebToken jwt;
    private final EnrollmentClient enrollmentClient;
    private final IdentityClient identityClient;

    @Inject
    public CertificateResource(
            CertificateService certificateService,
            CertificateMapper mapper,
            JsonWebToken jwt,
            @RestClient EnrollmentClient enrollmentClient,
            @RestClient IdentityClient identityClient
    ) {
        this.certificateService = certificateService;
        this.mapper = mapper;
        this.jwt = jwt;
        this.enrollmentClient = enrollmentClient;
        this.identityClient = identityClient;
    }

    @POST
    @Path("/request")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response requestCertificate(CertificateRequestBody body, @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        UUID userId = extractUserId();

        UserPublicDto user = identityClient.getUser(userId, authHeader);
        EnrollmentCompletionDto enrollment = enrollmentClient.getByUserAndCourse(userId, body.courseId());
        if (!enrollment.isCompleted()) {
            throw new BusinessRuleException("You have not completed this course yet");
        }

        Certificate certificate = certificateService.requestCertificate(userId, body.courseId(), user.email(), user.fullName());
        return Response.accepted(mapper.toResponse(certificate)).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public List<CertificateResponse> listMyCertificates() {
        UUID userId = extractUserId();
        return certificateService.findByUser(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GET
    @Path("/verify/{code}")
    @PermitAll
    public CertificateResponse verify(@PathParam("code") UUID code) {
        return certificateService.findByCode(code)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFound("Certificate not found"));
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }
}
