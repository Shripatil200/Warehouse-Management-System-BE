-- V2: Add operator_status column to the users table.
-- Tracks real-time floor operator availability (AVAILABLE / BUSY).
-- Default AVAILABLE so all pre-existing operators are immediately eligible
-- for task assignment after the migration runs.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS operator_status VARCHAR(20)
        NOT NULL DEFAULT 'AVAILABLE';

ALTER TABLE users
    ADD CONSTRAINT chk_operator_status
        CHECK (operator_status IN ('AVAILABLE', 'BUSY'));
