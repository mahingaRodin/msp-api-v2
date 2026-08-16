-- Refund approval workflow + subscription billing metadata
ALTER TABLE refunds
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN IF NOT EXISTS requested_by_user_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS restocked BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing rows stay COMPLETED (cashier-processed)
UPDATE refunds SET status = 'COMPLETED' WHERE status IS NULL OR status = '';

-- Trial default remains FREE_TRIAL; new apps use 15 days in app code
