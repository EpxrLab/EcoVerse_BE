-- Manual PostgreSQL migration to support minimum students per invited school.
-- Run after backing up data.

BEGIN;

ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS min_students_per_school integer;

COMMIT;
