-- V10__add_audit_consent_and_order_fixes.sql

-- ── Fix orders table ─────────────────────────────────────────────────────────
ALTER TABLE orders ADD COLUMN IF NOT EXISTS status    VARCHAR(50) DEFAULT 'PENDING';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);

CREATE INDEX IF NOT EXISTS idx_orders_tenant ON orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- ── Fix refunds table ────────────────────────────────────────────────────────
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_refunds_tenant ON refunds(tenant_id);

-- ── Fix shift_reports table ──────────────────────────────────────────────────
ALTER TABLE shift_reports ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_shift_reports_tenant ON shift_reports(tenant_id);

-- ── audit_logs ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID,
    actor_user_id UUID REFERENCES users(id),
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id   VARCHAR(100),
    details       TEXT,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant  ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor   ON audit_logs(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action  ON audit_logs(action);

-- Immutability trigger — audit rows can never be updated or deleted
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be modified or deleted';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS audit_immutability ON audit_logs;
CREATE TRIGGER audit_immutability
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- ── admin_consent_requests ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_consent_requests (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL,
    requesting_admin_id      UUID NOT NULL REFERENCES users(id),
    reason                   TEXT NOT NULL,
    requested_duration_hours INT  NOT NULL,
    status                   VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at             TIMESTAMP NOT NULL DEFAULT now(),
    granted_at               TIMESTAMP,
    expires_at               TIMESTAMP,
    revoked_at               TIMESTAMP,
    revoked_by_id            UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_consent_tenant  ON admin_consent_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_consent_admin   ON admin_consent_requests(requesting_admin_id);
CREATE INDEX IF NOT EXISTS idx_consent_status  ON admin_consent_requests(status);
CREATE INDEX IF NOT EXISTS idx_consent_expires ON admin_consent_requests(expires_at);
