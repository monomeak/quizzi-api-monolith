-- Flyway runs this once and records it in the flyway_schema_history table.
-- NEVER edit a migration that has already run anywhere: add a new V2, V3 ... instead.
-- "user" is a reserved word in PostgreSQL, hence app_user.

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(150) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);
