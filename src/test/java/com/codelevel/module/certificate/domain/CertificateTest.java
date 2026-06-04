package com.codelevel.module.certificate.domain;

import com.codelevel.module.certificate.persistence.entity.CertificateEntity;
import com.codelevel.module.certificate.persistence.entity.enums.CertificateStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CertificateTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CERT_CODE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CertificateEntity entityWith(CertificateStatus status) {
        CertificateEntity e = new CertificateEntity();
        e.id = 42L;
        e.userId = FIXED_USER_ID;
        e.courseId = 7L;
        e.userEmail = "user@example.com";
        e.userName = "John Doe";
        e.status = status;
        e.certificateCode = FIXED_CERT_CODE;
        e.verificationUrl = "https://cert.example.com/verify";
        e.errorMessage = null;
        e.requestedAt = LocalDateTime.now();
        e.sentAt = null;
        return e;
    }

    @Test
    void shouldThrowWhenEntityIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Certificate(null));
    }

    @Test
    void shouldCreateWithValidEntity() {
        assertDoesNotThrow(() -> new Certificate(entityWith(CertificateStatus.PENDING)));
    }

    // --- isTerminal ---

    @ParameterizedTest
    @EnumSource(value = CertificateStatus.class, names = {"SENT", "FAILED"})
    void shouldBeTerminalWhenStatusIsSentOrFailed(CertificateStatus status) {
        assertTrue(new Certificate(entityWith(status)).isTerminal());
    }

    @ParameterizedTest
    @EnumSource(value = CertificateStatus.class, names = {"PENDING", "PROCESSING"})
    void shouldNotBeTerminalWhenStatusIsPendingOrProcessing(CertificateStatus status) {
        assertFalse(new Certificate(entityWith(status)).isTerminal());
    }

    // --- isInProgress ---

    @ParameterizedTest
    @EnumSource(value = CertificateStatus.class, names = {"PENDING", "PROCESSING"})
    void shouldBeInProgressWhenStatusIsPendingOrProcessing(CertificateStatus status) {
        assertTrue(new Certificate(entityWith(status)).isInProgress());
    }

    @ParameterizedTest
    @EnumSource(value = CertificateStatus.class, names = {"SENT", "FAILED"})
    void shouldNotBeInProgressWhenStatusIsSentOrFailed(CertificateStatus status) {
        assertFalse(new Certificate(entityWith(status)).isInProgress());
    }

    // --- accessor methods (covers NULL_RETURNS and EMPTY_RETURNS mutations) ---

    @Test
    void shouldExposeEntityFields() {
        CertificateEntity entity = entityWith(CertificateStatus.SENT);
        LocalDateTime now = LocalDateTime.now();
        entity.sentAt = now;
        entity.errorMessage = "processing failed";

        Certificate cert = new Certificate(entity);

        assertEquals(42L, cert.id());
        assertEquals(FIXED_USER_ID, cert.userId());
        assertEquals(7L, cert.courseId());
        assertEquals("user@example.com", cert.userEmail());
        assertEquals("John Doe", cert.userName());
        assertEquals(CertificateStatus.SENT, cert.status());
        assertEquals(FIXED_CERT_CODE, cert.certificateCode());
        assertEquals("https://cert.example.com/verify", cert.verificationUrl());
        assertEquals("processing failed", cert.errorMessage());
        assertNotNull(cert.requestedAt());
        assertNotNull(cert.sentAt());
    }
}
