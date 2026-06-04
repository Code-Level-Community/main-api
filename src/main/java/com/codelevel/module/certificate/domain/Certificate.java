package com.codelevel.module.certificate.domain;

import com.codelevel.module.certificate.persistence.entity.CertificateEntity;
import com.codelevel.module.certificate.persistence.entity.enums.CertificateStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Certificate {

    private final CertificateEntity entity;

    public Certificate(CertificateEntity entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        this.entity = entity;
    }

    public Long id() { return entity.id; }
    public UUID userId() { return entity.userId; }
    public Long courseId() { return entity.courseId; }
    public String userEmail() { return entity.userEmail; }
    public String userName() { return entity.userName; }
    public CertificateStatus status() { return entity.status; }
    public UUID certificateCode() { return entity.certificateCode; }
    public String verificationUrl() { return entity.verificationUrl; }
    public String errorMessage() { return entity.errorMessage; }
    public LocalDateTime requestedAt() { return entity.requestedAt; }
    public LocalDateTime sentAt() { return entity.sentAt; }

    public boolean isTerminal() {
        return entity.status == CertificateStatus.SENT || entity.status == CertificateStatus.FAILED;
    }

    public boolean isInProgress() {
        return entity.status == CertificateStatus.PENDING || entity.status == CertificateStatus.PROCESSING;
    }
}
