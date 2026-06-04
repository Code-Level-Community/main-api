package com.codelevel.module.certificate.persistence.resource;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.domain.CertificateCode;
import com.codelevel.module.certificate.persistence.entity.CertificateEntity;
import com.codelevel.module.certificate.persistence.entity.enums.CertificateStatus;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CertificateService {

    private static final Logger log = Logger.getLogger(CertificateService.class);

    private final EventBus eventBus;

    public CertificateService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Creates the PENDING certificate in its own transaction, then fires the async event
     * after commit so the processor always finds a committed entity.
     */
    public Certificate requestCertificate(UUID userId, Long courseId, String userEmail, String userName) {
        CertificateEntity entity = persistPending(userId, courseId, userEmail, userName);
        log.infof("Certificate requested: id=%d userId=%s courseId=%d", entity.id, userId, courseId);
        eventBus.send("certificate.process", entity.id);
        return new Certificate(entity);
    }

    @Transactional
    public CertificateEntity persistPending(UUID userId, Long courseId, String userEmail, String userName) {
        CertificateEntity.findByUserAndCourse(userId, courseId).ifPresent(existing -> {
            if (!existing.status.equals(CertificateStatus.FAILED)) {
                throw new ResourceAlreadyExists("Certificate already requested for this course");
            }
        });

        var entity = new CertificateEntity();
        entity.userId = userId;
        entity.courseId = courseId;
        entity.userEmail = userEmail;
        entity.userName = userName;
        entity.status = CertificateStatus.PENDING;
        entity.requestedAt = LocalDateTime.now();
        entity.persist();
        return entity;
    }

    @Transactional
    public void markProcessing(Long certId) {
        findEntityOrThrow(certId).status = CertificateStatus.PROCESSING;
    }

    @Transactional
    public void markSent(Long certId, CertificateCode code, String verificationUrl) {
        CertificateEntity entity = findEntityOrThrow(certId);
        entity.status = CertificateStatus.SENT;
        entity.certificateCode = code.value();
        entity.verificationUrl = verificationUrl;
        entity.sentAt = LocalDateTime.now();
        log.infof("Certificate sent: id=%d code=%s", certId, code.display());
    }

    @Transactional
    public void markFailed(Long certId, String errorMessage) {
        CertificateEntity entity = findEntityOrThrow(certId);
        entity.status = CertificateStatus.FAILED;
        entity.errorMessage = errorMessage;
        log.errorf("Certificate failed: id=%d error=%s", certId, errorMessage);
    }

    public List<Certificate> findByUser(UUID userId) {
        return CertificateEntity.findByUserId(userId)
                .stream()
                .map(Certificate::new)
                .toList();
    }

    public Optional<Certificate> findByCode(UUID code) {
        return CertificateEntity.findByCertificateCode(code)
                .map(Certificate::new);
    }

    @Transactional
    public Certificate findById(Long certId) {
        return new Certificate(findEntityOrThrow(certId));
    }

    private CertificateEntity findEntityOrThrow(Long certId) {
        return (CertificateEntity) CertificateEntity.findByIdOptional(certId)
                .orElseThrow(() -> new ResourceNotFound("Certificate not found: " + certId));
    }
}
