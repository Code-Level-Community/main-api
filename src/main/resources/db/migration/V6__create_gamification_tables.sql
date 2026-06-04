-- Gamification module tables

CREATE SEQUENCE cl_xp_transaction_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_achievement_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_user_achievement_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_level_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_XP_TRANSACTION (
    id              BIGINT      PRIMARY KEY DEFAULT nextval('cl_xp_transaction_seq'),
    xpt_user_id     BIGINT      NOT NULL,
    xpt_xp_amount   BIGINT      NOT NULL,
    xpt_source      VARCHAR(50) NOT NULL,
    xpt_source_id   BIGINT      NOT NULL,
    xpt_description TEXT,
    xpt_created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_xp_transaction_user_source UNIQUE (xpt_user_id, xpt_source, xpt_source_id)
);

CREATE TABLE CL_LEVEL (
    id                  BIGINT      PRIMARY KEY DEFAULT nextval('cl_level_seq'),
    lvl_name            VARCHAR(100) NOT NULL,
    lvl_xp_required     BIGINT      NOT NULL,
    lvl_badge_icon_url  TEXT,
    lvl_created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_level_xp_required UNIQUE (lvl_xp_required)
);

CREATE TABLE CL_ACHIEVEMENT (
    id                  BIGINT      PRIMARY KEY DEFAULT nextval('cl_achievement_seq'),
    ac_name             VARCHAR(100) NOT NULL,
    ac_slug             VARCHAR(100) NOT NULL UNIQUE,
    ac_description      TEXT,
    ac_icon_url         TEXT,
    ac_trigger_type     VARCHAR(50) NOT NULL,
    ac_trigger_criteria VARCHAR(255) NOT NULL,
    ac_xp_reward        BIGINT      NOT NULL DEFAULT 0,
    ac_created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CL_USER_ACHIVEMENT (
    id                  BIGINT      PRIMARY KEY DEFAULT nextval('cl_user_achievement_seq'),
    ua_user_id          BIGINT      NOT NULL,
    ua_achievement_id   BIGINT      NOT NULL,
    ua_unlocked_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_achievement UNIQUE (ua_user_id, ua_achievement_id)
);

CREATE TABLE CL_USER_STREAK (
    us_user_id              BIGINT  PRIMARY KEY,
    us_current_streak_days  BIGINT  NOT NULL DEFAULT 0,
    us_last_activity_date   DATE    NOT NULL,
    us_longest_streak_days  BIGINT  NOT NULL DEFAULT 0,
    us_freeze_used_at       DATE,
    us_updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cl_xp_transaction_user_id ON CL_XP_TRANSACTION(xpt_user_id);
CREATE INDEX idx_cl_xp_transaction_source  ON CL_XP_TRANSACTION(xpt_source);
CREATE INDEX idx_cl_user_achievement_user  ON CL_USER_ACHIVEMENT(ua_user_id);
CREATE INDEX idx_cl_user_achievement_achv  ON CL_USER_ACHIVEMENT(ua_achievement_id);
