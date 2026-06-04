-- Reviews & Feedbacks module tables

CREATE SEQUENCE cl_course_review_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_lesson_feedback_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_COURSE_REVIEW (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('cl_course_review_seq'),
    cr_user_id      BIGINT          NOT NULL,
    cr_course_id    BIGINT          NOT NULL,
    cr_rating       INTEGER         NOT NULL CHECK (cr_rating BETWEEN 1 AND 5),
    cr_is_positive  BOOLEAN         NOT NULL,
    cr_comment      VARCHAR(1000),
    cr_created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cr_updated_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_review UNIQUE (cr_user_id, cr_course_id)
);

CREATE TABLE CL_LESSON_FEEDBACK (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('cl_lesson_feedback_seq'),
    lf_user_id      BIGINT          NOT NULL,
    lf_lesson_id    BIGINT          NOT NULL,
    lf_is_helpful   BOOLEAN         NOT NULL,
    lf_comment      VARCHAR(1000),
    lf_created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lesson_feedback UNIQUE (lf_user_id, lf_lesson_id)
);

CREATE INDEX idx_cl_course_review_course_id  ON CL_COURSE_REVIEW(cr_course_id);
CREATE INDEX idx_cl_course_review_user_id    ON CL_COURSE_REVIEW(cr_user_id);
CREATE INDEX idx_cl_lesson_feedback_lesson_id ON CL_LESSON_FEEDBACK(lf_lesson_id);
CREATE INDEX idx_cl_lesson_feedback_user_id   ON CL_LESSON_FEEDBACK(lf_user_id);
