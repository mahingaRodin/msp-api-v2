-- V8__add_tenant_id_to_stores_branches.sql
-- Links stores and branches to their owning Business tenant.

ALTER TABLE stores   ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);

CREATE INDEX IF NOT EXISTS idx_stores_tenant   ON stores(tenant_id);
CREATE INDEX IF NOT EXISTS idx_branches_tenant ON branches(tenant_id);
