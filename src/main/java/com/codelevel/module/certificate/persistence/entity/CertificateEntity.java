package com.codelevel.module.certificate.persistence.entity;

import com.codelevel.module.certificate.persistence.entity.enums.CertificateStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(name = "CL_CERTIFICATE")
@Entity
public class CertificateEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_certificate_seq", sequenceName = "cl_certificate_seq", allocationSize = 1)
    public Long id;

    @Column(name = "cert_user_id", nullable = false)
    public UUID userId;

    @Column(name = "cert_course_id", nullable = false)
    public Long courseId;

    @Column(name = "cert_user_email", nullable = false)
    public String userEmail;

    @Column(name = "cert_user_name", nullable = false)
    public String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_status", nullable = false)
    public CertificateStatus status = CertificateStatus.PENDING;

    @Column(name = "cert_certificate_code", unique = true)
    public UUID certificateCode;

    @Column(name = "cert_verification_url", columnDefinition = "text")
    public String verificationUrl;

    @Column(name = "cert_error_message", columnDefinition = "text")
    public String errorMessage;

    @Column(name = "cert_requested_at", nullable = false)
    public LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "cert_sent_at")
    public LocalDateTime sentAt;

    public static Optional<CertificateEntity> findByUserAndCourse(UUID userId, Long courseId) {
        return find("userId = ?1 and courseId = ?2", userId, courseId).firstResultOptional();
    }

    public static List<CertificateEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public static Optional<CertificateEntity> findByCertificateCode(UUID code) {
        return find("certificateCode", code).firstResultOptional();
    }
}
