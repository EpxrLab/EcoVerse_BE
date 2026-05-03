-- Manual PostgreSQL migration: per-school max invite quota for partnership campaigns.
-- Run after backing up data.

BEGIN;

ALTER TABLE campaign_schools_participate
    ADD COLUMN IF NOT EXISTS max_students_invited integer;

COMMIT;
