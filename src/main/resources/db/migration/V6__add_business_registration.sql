-- V6__add_business_registration.sql
-- Adds the Business and TenantRegistration tables and links users to their tenant.

-- ── businesses ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS businesses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL UNIQUE,
    business_name       VARCHAR(255) NOT NULL UNIQUE,
    legal_name          VARCHAR(255),
    registration_number VARCHAR(100),
    country             CHAR(2),
    industry            VARCHAR(100),
    description         TEXT,
    subscription_tier   VARCHAR(50) NOT NULL DEFAULT 'FREE_TRIAL',
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    owner_user_id       UUID NOT NULL REFERENCES users(id),
    trial_ends_at       TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_businesses_tenant_id ON businesses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_businesses_owner     ON businesses(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_businesses_status    ON businesses(status);

-- ── tenant_registrations ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_registrations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_first_name        VARCHAR(100) NOT NULL,
    owner_last_name         VARCHAR(100) NOT NULL,
    owner_email             VARCHAR(255) NOT NULL UNIQUE,
    owner_phone             VARCHAR(30),
    business_name           VARCHAR(255) NOT NULL,
    legal_name              VARCHAR(255),
    registration_number     VARCHAR(100),
    country                 CHAR(2),
    industry                VARCHAR(100),
    business_description    TEXT,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    admin_notes             TEXT,
    rejection_reason        TEXT,
    reviewed_by_id          UUID REFERENCES users(id),
    submitted_at            TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at             TIMESTAMP,
    provisioned_tenant_id   UUID
);

CREATE INDEX IF NOT EXISTS idx_reg_status ON tenant_registrations(status);
CREATE INDEX IF NOT EXISTS idx_reg_email  ON tenant_registrations(owner_email);

-- ── Add tenant_id to users ───────────────────────────────────────────────────
-- NULL for ROLE_SUPER_ADMIN; set for all tenant-scoped users.
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID;
