package com.codelevel.module.certificate.http.rest.mapper;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.http.rest.dto.CertificateResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CertificateMapper {

    public CertificateResponse toResponse(Certificate certificate) {
        return new CertificateResponse(
                certificate.id(),
                certificate.courseId(),
                certificate.status().name(),
                certificate.certificateCode() != null ? certificate.certificateCode().toString() : null,
                certificate.verificationUrl(),
                certificate.requestedAt(),
                certificate.sentAt()
        );
    }
}
