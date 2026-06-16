-- V11__add_outbox_events.sql
CREATE TABLE IF NOT EXISTS outbox_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    VARCHAR(100) NOT NULL,
    queue_url     VARCHAR(255) NOT NULL,
    payload       TEXT NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count   INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    processed_at  TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events(status, created_at);

-- Support table for business registration document upload keys
CREATE TABLE IF NOT EXISTS registration_documents (
    registration_id UUID NOT NULL REFERENCES tenant_registrations(id) ON DELETE CASCADE,
    document_key    VARCHAR(500) NOT NULL
);
