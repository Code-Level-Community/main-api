package com.codelevel.module.certificate.persistence.resource;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.domain.CertificateCode;
import com.codelevel.module.certificate.persistence.entity.CertificateEntity;
import com.codelevel.module.certificate.persistence.entity.enums.CertificateStatus;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CertificateServiceIT {

    @Inject
    CertificateService certificateService;

    @Inject
    EntityManager entityManager;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    // =====================================================================
    // persistPending — criação
    // =====================================================================

    @Test
    @TestTransaction
    void shouldCreateCertificateWithPendingStatus() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 1001L, "u@t.com", "User");

        assertNotNull(entity.id);
        assertEquals(CertificateStatus.PENDING, entity.status);
    }

    @Test
    @TestTransaction
    void shouldStoreUserIdAndCourseId() {
        CertificateEntity entity = certificateService.persistPending(uid(42), 1002L, "u@t.com", "User");

        assertEquals(uid(42), entity.userId);
        assertEquals(1002L, entity.courseId);
    }

    @Test
    @TestTransaction
    void shouldStoreUserEmailAndName() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 1003L, "email@test.com", "John Doe");

        assertEquals("email@test.com", entity.userEmail);
        assertEquals("John Doe", entity.userName);
    }

    @Test
    @TestTransaction
    void shouldSetRequestedAtOnCreation() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 1004L, "u@t.com", "User");

        assertNotNull(entity.requestedAt);
    }

    @Test
    @TestTransaction
    void shouldNotSetCertificateCodeOnCreation() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 1005L, "u@t.com", "User");

        assertNull(entity.certificateCode);
    }

    @Test
    @TestTransaction
    void shouldNotSetSentAtOnCreation() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 1006L, "u@t.com", "User");

        assertNull(entity.sentAt);
    }

    // =====================================================================
    // persistPending — regras de duplicata
    // =====================================================================

    @Test
    @TestTransaction
    void shouldThrowWhenCertificateAlreadyPending() {
        certificateService.persistPending(uid(1), 2001L, "u@t.com", "User");
        entityManager.flush();

        assertThrows(ResourceAlreadyExists.class,
                () -> certificateService.persistPending(uid(1), 2001L, "u@t.com", "User"));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenCertificateAlreadyProcessing() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 2002L, "u@t.com", "User");
        entity.status = CertificateStatus.PROCESSING;
        entityManager.flush();

        assertThrows(ResourceAlreadyExists.class,
                () -> certificateService.persistPending(uid(1), 2002L, "u@t.com", "User"));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenCertificateAlreadySent() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 2003L, "u@t.com", "User");
        entity.status = CertificateStatus.SENT;
        entityManager.flush();

        assertThrows(ResourceAlreadyExists.class,
                () -> certificateService.persistPending(uid(1), 2003L, "u@t.com", "User"));
    }

    @Test
    @TestTransaction
    void shouldAllowNewRequestAfterPreviousFailed() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 2004L, "u@t.com", "User");
        certificateService.markFailed(entity.id, "Processing error");
        entityManager.flush();

        assertDoesNotThrow(
                () -> certificateService.persistPending(uid(1), 2004L, "u@t.com", "User"));
    }

    // =====================================================================
    // Transições de status
    // =====================================================================

    @Test
    @TestTransaction
    void shouldMarkProcessing() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 3001L, "u@t.com", "User");
        entityManager.flush();

        certificateService.markProcessing(entity.id);
        entityManager.flush();
        entityManager.clear();

        Certificate cert = certificateService.findById(entity.id);
        assertEquals(CertificateStatus.PROCESSING, cert.status());
    }

    @Test
    @TestTransaction
    void shouldMarkSentWithCertificateCode() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 3002L, "u@t.com", "User");
        entityManager.flush();

        CertificateCode code = CertificateCode.generate();
        certificateService.markSent(entity.id, code, "/api/certificates/verify/" + code.value());
        entityManager.flush();
        entityManager.clear();

        Certificate cert = certificateService.findById(entity.id);
        assertEquals(CertificateStatus.SENT, cert.status());
        assertEquals(code.value(), cert.certificateCode());
    }

    @Test
    @TestTransaction
    void shouldMarkSentWithVerificationUrl() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 3003L, "u@t.com", "User");
        entityManager.flush();

        CertificateCode code = CertificateCode.generate();
        String url = "/api/certificates/verify/" + code.value();
        certificateService.markSent(entity.id, code, url);
        entityManager.flush();
        entityManager.clear();

        Certificate cert = certificateService.findById(entity.id);
        assertEquals(url, cert.verificationUrl());
    }

    @Test
    @TestTransaction
    void shouldMarkSentWithSentAt() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 3004L, "u@t.com", "User");
        entityManager.flush();

        CertificateCode code = CertificateCode.generate();
        certificateService.markSent(entity.id, code, "/api/certificates/verify/" + code.value());
        entityManager.flush();
        entityManager.clear();

        Certificate cert = certificateService.findById(entity.id);
        assertNotNull(cert.sentAt());
    }

    @Test
    @TestTransaction
    void shouldMarkFailedWithErrorMessage() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 3005L, "u@t.com", "User");
        entityManager.flush();

        certificateService.markFailed(entity.id, "Email delivery failed");
        entityManager.flush();
        entityManager.clear();

        Certificate cert = certificateService.findById(entity.id);
        assertEquals(CertificateStatus.FAILED, cert.status());
        assertEquals("Email delivery failed", cert.errorMessage());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenMarkingNonExistentCertificateAsProcessing() {
        assertThrows(ResourceNotFound.class,
                () -> certificateService.markProcessing(999999L));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenMarkingNonExistentCertificateAsSent() {
        assertThrows(ResourceNotFound.class,
                () -> certificateService.markSent(999999L, CertificateCode.generate(), "/url"));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenMarkingNonExistentCertificateAsFailed() {
        assertThrows(ResourceNotFound.class,
                () -> certificateService.markFailed(999999L, "error"));
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    @Test
    @TestTransaction
    void shouldFindAllCertificatesForUser() {
        certificateService.persistPending(uid(55), 4001L, "u@t.com", "User");
        certificateService.persistPending(uid(55), 4002L, "u@t.com", "User");
        entityManager.flush();

        List<Certificate> certs = certificateService.findByUser(uid(55));

        assertEquals(2, certs.size());
    }

    @Test
    @TestTransaction
    void shouldReturnEmptyListForUserWithNoCertificates() {
        List<Certificate> certs = certificateService.findByUser(uid(99999));

        assertTrue(certs.isEmpty());
    }

    @Test
    @TestTransaction
    void shouldFindCertificateByCode() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 4003L, "u@t.com", "User");
        UUID code = UUID.randomUUID();
        entity.certificateCode = code;
        entityManager.flush();
        entityManager.clear();

        Optional<Certificate> found = certificateService.findByCode(code);

        assertTrue(found.isPresent());
        assertEquals(code, found.get().certificateCode());
    }

    @Test
    @TestTransaction
    void shouldReturnEmptyWhenCodeNotFound() {
        Optional<Certificate> found = certificateService.findByCode(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    @TestTransaction
    void shouldFindCertificateById() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 4004L, "u@t.com", "User");
        entityManager.flush();
        entityManager.clear();

        Certificate cert = certificateService.findById(entity.id);

        assertNotNull(cert);
        assertEquals(entity.id, cert.id());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenFindByIdNotFound() {
        assertThrows(ResourceNotFound.class,
                () -> certificateService.findById(999999L));
    }

    // =====================================================================
    // Domain — Certificate
    // =====================================================================

    @Test
    @TestTransaction
    void shouldReturnTrueForIsInProgressWhenPending() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 5001L, "u@t.com", "User");
        Certificate cert = new com.codelevel.module.certificate.domain.Certificate(entity);

        assertTrue(cert.isInProgress());
        assertFalse(cert.isTerminal());
    }

    @Test
    @TestTransaction
    void shouldReturnTrueForIsTerminalWhenSent() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 5002L, "u@t.com", "User");
        entity.status = CertificateStatus.SENT;
        Certificate cert = new com.codelevel.module.certificate.domain.Certificate(entity);

        assertTrue(cert.isTerminal());
        assertFalse(cert.isInProgress());
    }

    @Test
    @TestTransaction
    void shouldReturnTrueForIsTerminalWhenFailed() {
        CertificateEntity entity = certificateService.persistPending(uid(1), 5003L, "u@t.com", "User");
        entity.status = CertificateStatus.FAILED;
        Certificate cert = new com.codelevel.module.certificate.domain.Certificate(entity);

        assertTrue(cert.isTerminal());
        assertFalse(cert.isInProgress());
    }
}
