-- Course module tables

CREATE SEQUENCE cl_course_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_module_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_lesson_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_exercise_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_COURSE (
    id                      BIGINT          PRIMARY KEY DEFAULT nextval('cl_course_seq'),
    co_instructor_id        BIGINT,
    co_title                VARCHAR(255)    NOT NULL,
    co_description          TEXT            NOT NULL,
    co_thumbnail_url        VARCHAR(512)    NOT NULL,
    co_difficulty_level     VARCHAR(50)     NOT NULL,
    co_total_duration_minutes BIGINT,
    co_status               VARCHAR(50)     DEFAULT 'DRAFT',
    co_approval_threshold   DOUBLE PRECISION,
    co_total_lessons        BIGINT,
    co_total_enrollments    BIGINT,
    co_average_rating       DOUBLE PRECISION,
    co_published_at         TIMESTAMP,
    co_created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    co_updated_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CL_MODULE (
    id                  BIGINT          PRIMARY KEY DEFAULT nextval('cl_module_seq'),
    m_course_id         BIGINT          NOT NULL REFERENCES CL_COURSE(id) ON DELETE CASCADE,
    m_title             VARCHAR(255)    NOT NULL,
    m_description       TEXT,
    m_order_position    BIGINT,
    m_created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    m_updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CL_LESSON (
    id                          BIGINT          PRIMARY KEY DEFAULT nextval('cl_lesson_seq'),
    l_module_id                 BIGINT          NOT NULL REFERENCES CL_MODULE(id) ON DELETE CASCADE,
    l_title                     VARCHAR(255)    NOT NULL,
    l_description               TEXT,
    l_content_type              VARCHAR(50),
    l_video_url                 TEXT,
    l_video_duration_seconds    BIGINT,
    l_text_content              TEXT,
    l_order_position            BIGINT,
    l_xp_reward                 BIGINT,
    l_has_exercises             BOOLEAN         NOT NULL DEFAULT FALSE,
    l_created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    l_updated_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CL_EXERCISE (
    id                  BIGINT          PRIMARY KEY DEFAULT nextval('cl_exercise_seq'),
    e_lesson_id         BIGINT          NOT NULL REFERENCES CL_LESSON(id) ON DELETE CASCADE,
    e_title             VARCHAR(255)    NOT NULL,
    e_description       TEXT,
    e_quiz_data         JSONB,
    e_code_template     TEXT,
    e_test_cases        JSONB,
    e_max_attempts      BIGINT,
    e_xp_reward         BIGINT,
    e_created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE cl_course_category_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_course_tag_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_COURSE_CATEGORY (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('cl_course_category_seq'),
    ca_name         VARCHAR(100)    NOT NULL,
    ca_slug         VARCHAR(100)    NOT NULL UNIQUE,
    ca_icon_url     VARCHAR(512),
    ca_created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CL_COURSE_TAG (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('cl_course_tag_seq'),
    ct_name         VARCHAR(100)    NOT NULL,
    ct_slug         VARCHAR(100)    NOT NULL UNIQUE,
    ct_created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CL_COURSE_CATEGORY_MAPPING (
    ccm_course_id       BIGINT  NOT NULL REFERENCES CL_COURSE(id) ON DELETE CASCADE,
    ccm_category_id     BIGINT  NOT NULL REFERENCES CL_COURSE_CATEGORY(id) ON DELETE CASCADE,
    PRIMARY KEY (ccm_course_id, ccm_category_id)
);

CREATE TABLE CL_COURSE_TAG_MAPPING (
    ctm_course_id   BIGINT  NOT NULL REFERENCES CL_COURSE(id) ON DELETE CASCADE,
    ctm_tag_id      BIGINT  NOT NULL REFERENCES CL_COURSE_TAG(id) ON DELETE CASCADE,
    PRIMARY KEY (ctm_course_id, ctm_tag_id)
);

CREATE INDEX idx_cl_module_course_id ON CL_MODULE(m_course_id);
CREATE INDEX idx_cl_lesson_module_id ON CL_LESSON(l_module_id);
CREATE INDEX idx_cl_exercise_lesson_id ON CL_EXERCISE(e_lesson_id);
CREATE INDEX idx_cl_course_instructor_id ON CL_COURSE(co_instructor_id);
CREATE INDEX idx_cl_course_status ON CL_COURSE(co_status);
