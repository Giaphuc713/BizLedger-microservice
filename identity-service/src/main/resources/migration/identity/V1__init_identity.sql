-- =============================================================
-- Identity Service — Database Schema (Plural Aligned)
-- V1__init_identity.sql
-- =============================================================

CREATE TABLE tenant (
                         id          UUID PRIMARY KEY,
                         name        VARCHAR(255) NOT NULL,
                         code        VARCHAR(100) NOT NULL UNIQUE,
                         status      VARCHAR(50) NOT NULL,
                         created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tenant_code ON tenant(code);

-- =============================================================

CREATE TABLE "user" (
                       id             UUID PRIMARY KEY,
                       tenant_id      UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                       email          VARCHAR(255) NOT NULL UNIQUE,
                       password_hash  VARCHAR(255) NOT NULL,
                       full_name      VARCHAR(255) NOT NULL,
                       active         BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at     TIMESTAMPTZ NOT NULL,
                       mfa_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
                       mfa_secret     VARCHAR(255),
                       sso_provider   VARCHAR(255),
                       sso_subject    VARCHAR(255)
);

CREATE INDEX idx_user_tenant ON "user"(tenant_id);

-- =============================================================

CREATE TABLE cdc_subscriptions (
                                    id               UUID PRIMARY KEY,
                                    endpoint         VARCHAR(255) NOT NULL,
                                    monitored_tables VARCHAR(255) NOT NULL,
                                    status           VARCHAR(50) NOT NULL,
                                    last_sync_at     TIMESTAMPTZ
);

-- =============================================================

CREATE TABLE role (
                       id         UUID PRIMARY KEY,
                       tenant_id  UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                       code       VARCHAR(100) NOT NULL,
                       name       VARCHAR(255) NOT NULL
);

CREATE INDEX idx_role_tenant_code ON role(tenant_id, code);

-- =============================================================

CREATE TABLE permission (
                             id           UUID PRIMARY KEY,
                             code         VARCHAR(100) NOT NULL UNIQUE,
                             description  VARCHAR(255) NOT NULL
);

-- =============================================================

CREATE TABLE user_role (
                            user_id  UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
                            role_id  UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- =============================================================

CREATE TABLE role_permission (
                                  role_id        UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
                                  permission_id  UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

-- =============================================================
-- Outbox Pattern
-- =============================================================
CREATE TABLE outbox_event (
                               id           UUID PRIMARY KEY,
                               event_id     VARCHAR(100) NOT NULL UNIQUE,
                               event_type   VARCHAR(100) NOT NULL,
                               topic        VARCHAR(200) NOT NULL,
                               payload      TEXT NOT NULL,
                               status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               retry_count  INT NOT NULL DEFAULT 0,
                               created_at   TIMESTAMPTZ NOT NULL,
                               published_at TIMESTAMPTZ
);

CREATE INDEX idx_identity_outbox ON outbox_event(status, created_at);

-- =============================================================
-- API Keys (M2M authentication support - FR-EXT-02)
-- =============================================================
CREATE TABLE api_key (
                          id            UUID PRIMARY KEY,
                          tenant_id     UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                          user_id       UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
                          name          VARCHAR(255) NOT NULL,
                          key_hash      VARCHAR(255) NOT NULL UNIQUE,
                          scopes        VARCHAR(500) NOT NULL,
                          created_at    TIMESTAMPTZ NOT NULL,
                          expires_at    TIMESTAMPTZ,
                          last_used_at  TIMESTAMPTZ
);

CREATE INDEX idx_api_key_hash ON api_key(key_hash);
CREATE INDEX idx_api_key_tenant ON api_key(tenant_id);

-- =============================================================
-- Feature Flags (Tenant Rollouts & Kill switches - FR-EXT-04)
-- =============================================================
CREATE TABLE feature_flag (
                               id           UUID PRIMARY KEY,
                               tenant_id    UUID REFERENCES tenant(id) ON DELETE CASCADE, -- Null means Global
                               key          VARCHAR(255) NOT NULL,
                               description  TEXT,
                               enabled      BOOLEAN NOT NULL DEFAULT TRUE,
                               rules        TEXT, -- JSON payload for custom percentage rollouts / allow-lists
                               created_at   TIMESTAMPTZ NOT NULL,
                               updated_at   TIMESTAMPTZ,
                               UNIQUE(tenant_id, key)
);

CREATE INDEX idx_feature_flag_tenant ON feature_flag(tenant_id);

