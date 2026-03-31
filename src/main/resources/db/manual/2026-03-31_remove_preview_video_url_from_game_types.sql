-- Manual PostgreSQL migration to remove preview video URL from game types.
-- Run after backing up data.

BEGIN;

ALTER TABLE game_types
    DROP COLUMN IF EXISTS preview_video_url;

COMMIT;

