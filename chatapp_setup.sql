
--  ChatApp Database Schema


DROP DATABASE IF EXISTS chatapp;
CREATE DATABASE chatapp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chatapp;

-- ── users table ──────────────────────────────────────────────
-- NEW COLUMNS: role, status
CREATE TABLE users (
    id         INT          NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(10)  NOT NULL DEFAULT 'user',    -- 'user' or 'admin'
    status     VARCHAR(10)  NOT NULL DEFAULT 'active',  -- 'active' or 'banned'
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_username ON users (username);

-- Default admin account (username: admin, password: admin123)
INSERT INTO users (username, password, role, status)
VALUES ('admin', 'admin123', 'admin', 'active');

-- ── messages table ───────────────────────────────────────────
CREATE TABLE messages (
    id        INT           NOT NULL AUTO_INCREMENT,
    sender    VARCHAR(50)   NOT NULL,
    receiver  VARCHAR(50)   NOT NULL DEFAULT 'ALL',
    message   TEXT          NOT NULL,
    timestamp TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (sender) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_timestamp ON messages (timestamp);
CREATE INDEX idx_sender    ON messages (sender);

-- ── Verify ───────────────────────────────────────────────────
SHOW TABLES;
DESCRIBE users;
DESCRIBE messages;
