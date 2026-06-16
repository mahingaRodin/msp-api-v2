-- V9__fix_product_category_tenant_scoping.sql
-- 1. SKU uniqueness is now per-store, not global.
-- 2. Products and categories gain a tenant_id column.

-- Drop the old global unique constraint on sku
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_sku_key;

-- Add per-store SKU uniqueness
ALTER TABLE products ADD CONSTRAINT uq_product_sku_per_store UNIQUE (sku, store_id);

-- Add tenant_id to products and categories
ALTER TABLE products    ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);
ALTER TABLE categories  ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES businesses(tenant_id);

CREATE INDEX IF NOT EXISTS idx_products_tenant   ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_categories_tenant ON categories(tenant_id);
