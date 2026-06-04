package com.codelevel.module.certificate.http.rest.dto;

import java.time.LocalDateTime;

public record CertificateResponse(
        Long id,
        Long courseId,
        String status,
        String certificateCode,
        String verificationUrl,
        LocalDateTime requestedAt,
        LocalDateTime sentAt
) {}
