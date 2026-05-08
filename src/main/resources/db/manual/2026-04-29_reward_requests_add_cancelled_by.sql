-- Manual PostgreSQL migration to store cancel actor for reward requests.
-- Run after backing up data.

BEGIN;

ALTER TABLE reward_requests
    ADD COLUMN IF NOT EXISTS cancelled_by uuid;

ALTER TABLE reward_requests
    DROP CONSTRAINT IF EXISTS fk_reward_requests_cancelled_by;

ALTER TABLE reward_requests
    ADD CONSTRAINT fk_reward_requests_cancelled_by
    FOREIGN KEY (cancelled_by) REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_reward_requests_cancelled_by
    ON reward_requests(cancelled_by);

COMMIT;
