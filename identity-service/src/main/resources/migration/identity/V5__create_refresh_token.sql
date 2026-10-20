CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
