-- Identity module tables: users, roles, permissions, join tables, refresh tokens

CREATE SEQUENCE cl_user_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_role_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cl_permission_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE CL_USER (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('cl_user_seq'),
    u_username      VARCHAR(50)  NOT NULL UNIQUE,
    u_pass          VARCHAR(255) NOT NULL,
    u_full_name     VARCHAR(255),
    u_avatar_url    TEXT,
    u_email         VARCHAR(255) NOT NULL UNIQUE,
    u_public_id     UUID         UNIQUE,
    u_last_login    TIMESTAMP,
    u_created_at    TIMESTAMP,
    u_updated_at    TIMESTAMP,
    u_enabled       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE CL_ROLE (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('cl_role_seq'),
    r_name          VARCHAR(100) NOT NULL UNIQUE,
    r_description   VARCHAR(255)
);

CREATE TABLE CL_PERMISSION (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('cl_permission_seq'),
    p_name          VARCHAR(100) NOT NULL UNIQUE,
    p_description   VARCHAR(255)
);

CREATE TABLE CL_USER_ROLE (
    user_id         BIGINT NOT NULL REFERENCES CL_USER(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES CL_ROLE(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE CL_ROLE_PERMISSION (
    role_id         BIGINT NOT NULL REFERENCES CL_ROLE(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES CL_PERMISSION(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE CL_REFRESH_TOKEN (
    id          BIGSERIAL    PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    username    VARCHAR(50)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_cl_user_username      ON CL_USER(u_username);
CREATE INDEX idx_cl_user_email         ON CL_USER(u_email);
CREATE INDEX idx_cl_user_public_id     ON CL_USER(u_public_id);
CREATE INDEX idx_cl_user_enabled       ON CL_USER(u_enabled);
CREATE INDEX idx_cl_user_role_user     ON CL_USER_ROLE(user_id);
CREATE INDEX idx_cl_user_role_role     ON CL_USER_ROLE(role_id);
CREATE INDEX idx_cl_refresh_token_tok  ON CL_REFRESH_TOKEN(token);
CREATE INDEX idx_cl_refresh_token_usr  ON CL_REFRESH_TOKEN(username);
CREATE INDEX idx_cl_refresh_token_rev  ON CL_REFRESH_TOKEN(revoked);

-- Seed: base roles required by @PrePersist on UserEntity
INSERT INTO CL_ROLE (id, r_name, r_description) VALUES
    (1, 'ROLE_USER',       'Default role assigned to all registered users'),
    (2, 'ROLE_INSTRUCTOR', 'Course instructor with content creation rights'),
    (3, 'ROLE_ADMIN',      'Platform administrator with full access');