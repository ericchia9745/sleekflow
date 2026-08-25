CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(60)  NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    -- Format: <algorithm>$<salt>$<digest>. Storing the algorithm alongside the
    -- digest lets a stronger hash be introduced without a migration: existing
    -- rows keep verifying under their own algorithm and are re-hashed on the
    -- owner's next successful login.
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE = InnoDB;

CREATE TABLE user_sessions (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    -- The SHA-256 of the token, never the token itself. Someone who reads this
    -- table cannot use what they find to authenticate.
    token_hash   CHAR(64)    NOT NULL,
    user_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    expires_at   DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6) NOT NULL,
    revoked_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Every authenticated request looks a session up by token hash, so that index
-- carries the login path; the other supports listing and revoking a user's
-- sessions, and sweeping expired rows.
CREATE INDEX idx_user_sessions_user ON user_sessions (user_id, expires_at);
CREATE INDEX idx_user_sessions_expires ON user_sessions (expires_at);
