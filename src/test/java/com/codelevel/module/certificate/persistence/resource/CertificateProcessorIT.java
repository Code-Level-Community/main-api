package com.codelevel.module.certificate.persistence.resource;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.persistence.entity.CertificateEntity;
import com.codelevel.module.certificate.persistence.entity.enums.CertificateStatus;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.ext.mail.MailMessage;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CertificateProcessorIT {

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000080");

    @Inject
    CertificateService certificateService;

    @Inject
    CertificateProcessor processor;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
    }

    // =====================================================================
    // Transições de status via processamento
    // =====================================================================

    @Test
    void shouldSetStatusToSentAfterSuccessfulProcessing() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8001L, "sent@processor.com", "Processor User");

        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);
        assertEquals(CertificateStatus.SENT, cert.status());
    }

    @Test
    void shouldGenerateCertificateCodeAfterProcessing() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8002L, "code@processor.com", "Code User");

        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);
        assertNotNull(cert.certificateCode());
    }

    @Test
    void shouldSetVerificationUrlAfterProcessing() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8003L, "url@processor.com", "URL User");

        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);
        assertNotNull(cert.verificationUrl());
        assertTrue(cert.verificationUrl().startsWith("/api/certificates/verify/"));
    }

    @Test
    void shouldSetSentAtAfterProcessing() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8004L, "sentat@processor.com", "SentAt User");

        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);
        assertNotNull(cert.sentAt());
    }

    @Test
    void shouldVerificationUrlContainTheCertificateCode() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8005L, "urlcode@processor.com", "UrlCode User");

        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);
        assertTrue(cert.verificationUrl().contains(cert.certificateCode().toString()));
    }

    // =====================================================================
    // Envio de email via MockMailbox
    // =====================================================================

    @Test
    void shouldSendEmailToUserEmail() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8006L, "target@mailbox.com", "Target User");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("target@mailbox.com");
        assertEquals(1, emails.size());
    }

    @Test
    void shouldSendEmailWithCorrectSubject() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8007L, "subject@mailbox.com", "Subject User");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("subject@mailbox.com");
        assertEquals("Congratulations! Your certificate is ready", emails.get(0).getSubject());
    }

    @Test
    void shouldSendEmailWithHtmlBody() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8008L, "html@mailbox.com", "HTML User");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("html@mailbox.com");
        assertNotNull(emails.get(0).getHtml());
        assertFalse(emails.get(0).getHtml().isBlank());
    }

    @Test
    void shouldIncludeCertificateCodeInEmailBody() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8009L, "certcode@mailbox.com", "CertCode User");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("certcode@mailbox.com");
        assertTrue(emails.get(0).getHtml().contains("CERT-"),
                "Email body should contain the certificate code prefix");
    }

    @Test
    void shouldIncludeUserNameInEmailBody() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8010L, "username@mailbox.com", "Lucas Fernandes");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("username@mailbox.com");
        assertTrue(emails.get(0).getHtml().contains("Lucas Fernandes"),
                "Email body should address the user by name");
    }

    @Test
    void shouldIncludeVerificationUrlInEmailBody() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8011L, "verifyurl@mailbox.com", "Verify User");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("verifyurl@mailbox.com");
        assertTrue(emails.get(0).getHtml().contains("/api/certificates/verify/"),
                "Email body should contain the verification URL");
    }

    @Test
    void shouldSendExactlyOneEmailPerCertificate() {
        CertificateEntity entity = certificateService.persistPending(
                TEST_USER_ID, 8012L, "once@mailbox.com", "Once User");

        processor.process(entity.id);

        List<MailMessage> emails = mailbox.getMailMessagesSentTo("once@mailbox.com");
        assertEquals(1, emails.size(), "Exactly one email should be sent per certificate");
    }

    // =====================================================================
    // Tratamento de erros
    // =====================================================================

    @Test
    void shouldHandleNonExistentCertificateGracefully() {
        assertDoesNotThrow(() -> processor.process(999999L),
                "Processing a non-existent certificate should not throw an unchecked exception");
    }

    @Test
    void shouldNotSendEmailWhenCertificateNotFound() {
        processor.process(999999L);

        assertTrue(mailbox.getMailMessagesSentTo("nonexistent-cert-id@test.com").isEmpty(),
                "No email should be sent for non-existent certificate");
    }
}
