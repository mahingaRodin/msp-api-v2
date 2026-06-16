-- V4__add_discharged_at.sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS discharged_at TIMESTAMP NULL;