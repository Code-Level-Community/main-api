package com.codelevel.module.certificate.persistence.resource;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.domain.CertificateCode;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CertificateProcessor {

    private static final Logger log = Logger.getLogger(CertificateProcessor.class);

    private final CertificateService certificateService;
    private final Mailer mailer;

    @Location("certificate-email")
    Template certificateEmail;

    public CertificateProcessor(CertificateService certificateService, Mailer mailer) {
        this.certificateService = certificateService;
        this.mailer = mailer;
    }

    @ConsumeEvent("certificate.process")
    @Blocking
    public void process(Long certId) {
        log.infof("Processing certificate id=%d", certId);

        try {
            certificateService.markProcessing(certId);

            Certificate cert = certificateService.findById(certId);
            CertificateCode code = CertificateCode.generate();
            String verificationUrl = "/api/certificates/verify/" + code.value();

            String emailBody = certificateEmail
                    .data("userName", cert.userName())
                    .data("courseId", cert.courseId())
                    .data("certificateCode", code.display())
                    .data("verificationUrl", verificationUrl)
                    .render();

            mailer.send(
                    Mail.withHtml(
                            cert.userEmail(),
                            "Congratulations! Your certificate is ready",
                            emailBody
                    )
            );

            certificateService.markSent(certId, code, verificationUrl);

        } catch (Exception e) {
            log.errorf("Failed to process certificate id=%d: %s", certId, e.getMessage());
            try {
                certificateService.markFailed(certId, e.getMessage());
            } catch (Exception inner) {
                log.errorf("Could not persist failure status for certificate id=%d", certId);
            }
        }
    }
}
