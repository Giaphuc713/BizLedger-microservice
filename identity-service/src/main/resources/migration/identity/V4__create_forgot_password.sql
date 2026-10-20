CREATE TABLE forgot_password (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);

CREATE INDEX idx_forgot_password_email ON forgot_password(email);
