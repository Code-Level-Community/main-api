-- Integration module tables

CREATE SEQUENCE cl_social_media_post_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_SOCIAL_MEDIA_POST (
    id                  BIGINT          PRIMARY KEY DEFAULT nextval('cl_social_media_post_seq'),
    smp_user_id         BIGINT          NOT NULL,
    smp_platform        VARCHAR(50)     NOT NULL,
    smp_post_type       VARCHAR(50)     NOT NULL,
    smp_reference_id    BIGINT,
    smp_content         TEXT            NOT NULL,
    smp_media_url       TEXT,
    smp_status          VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    smp_error_message   TEXT,
    smp_scheduled_at    TIMESTAMP,
    smp_posted_at       TIMESTAMP,
    smp_created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cl_social_media_post_user_id  ON CL_SOCIAL_MEDIA_POST(smp_user_id);
CREATE INDEX idx_cl_social_media_post_platform ON CL_SOCIAL_MEDIA_POST(smp_platform);
CREATE INDEX idx_cl_social_media_post_status   ON CL_SOCIAL_MEDIA_POST(smp_status);
CREATE INDEX idx_cl_social_media_post_type     ON CL_SOCIAL_MEDIA_POST(smp_post_type);