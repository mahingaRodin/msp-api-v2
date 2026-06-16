-- V7__add_customer_global_identity.sql
-- Customers are now global platform accounts.
-- Store-specific relationship is tracked in customer_store_relationships.

-- Remove the old store_id column if it was added by a previous migration attempt
ALTER TABLE customers DROP COLUMN IF EXISTS store_id;

-- Add password column (for self-registered customers)
ALTER TABLE customers ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Enforce global email uniqueness
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_email_key;
ALTER TABLE customers ADD CONSTRAINT customers_email_unique UNIQUE (email);

-- ── customer_store_relationships ─────────────────────────────────────────────
-- Created automatically when a customer places their first order at a store.
CREATE TABLE IF NOT EXISTS customer_store_relationships (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id             UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    store_id                UUID NOT NULL REFERENCES stores(id)    ON DELETE CASCADE,
    first_interaction_at    TIMESTAMP NOT NULL DEFAULT now(),
    last_interaction_at     TIMESTAMP,
    notes                   TEXT,
    CONSTRAINT uq_customer_store UNIQUE (customer_id, store_id)
);

CREATE INDEX IF NOT EXISTS idx_csr_customer ON customer_store_relationships(customer_id);
CREATE INDEX IF NOT EXISTS idx_csr_store    ON customer_store_relationships(store_id);
