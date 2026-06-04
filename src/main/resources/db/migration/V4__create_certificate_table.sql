CREATE SEQUENCE cl_certificate_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_CERTIFICATE (
    id                   BIGINT       PRIMARY KEY,
    cert_user_id         BIGINT       NOT NULL,
    cert_course_id       BIGINT       NOT NULL,
    cert_user_email      VARCHAR(255) NOT NULL,
    cert_user_name       VARCHAR(255) NOT NULL,
    cert_status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    cert_certificate_code UUID        UNIQUE,
    cert_verification_url TEXT,
    cert_error_message   TEXT,
    cert_requested_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cert_sent_at         TIMESTAMP
);

CREATE INDEX idx_cl_cert_user_id   ON CL_CERTIFICATE (cert_user_id);
CREATE INDEX idx_cl_cert_course_id ON CL_CERTIFICATE (cert_course_id);
CREATE INDEX idx_cl_cert_code      ON CL_CERTIFICATE (cert_certificate_code);
