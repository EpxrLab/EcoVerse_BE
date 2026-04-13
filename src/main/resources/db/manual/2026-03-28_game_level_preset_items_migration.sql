-- Manual PostgreSQL migration for refactoring game_level_presets -> game_level_preset_items.
-- Run in a maintenance window and backup before executing.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Create new child table for per-level settings.
CREATE TABLE IF NOT EXISTS game_level_preset_items (
    id uuid PRIMARY KEY,
    preset_id uuid NOT NULL,
    level_number integer NOT NULL,
    item_count integer NOT NULL,
    time_limit_seconds integer NOT NULL,
    score_per_correct integer NOT NULL,
    lives integer,
    config_json jsonb,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT fk_glpi_preset FOREIGN KEY (preset_id) REFERENCES game_level_presets(id) ON DELETE CASCADE,
    CONSTRAINT uk_glpi_preset_level UNIQUE (preset_id, level_number)
);

CREATE INDEX IF NOT EXISTS idx_glpi_preset_id ON game_level_preset_items(preset_id);
CREATE INDEX IF NOT EXISTS idx_glpi_level_number ON game_level_preset_items(level_number);

-- 2) Build a parent mapping: pick one surviving preset row per (game_type_id, difficulty).
CREATE TEMP TABLE tmp_glp_parent AS
SELECT game_type_id,
       difficulty,
       MIN(id) AS parent_id
FROM game_level_presets
GROUP BY game_type_id, difficulty;

-- 3) Backfill child items from legacy columns.
INSERT INTO game_level_preset_items (
    id,
    preset_id,
    level_number,
    item_count,
    time_limit_seconds,
    score_per_correct,
    lives,
    config_json,
    created_at,
    updated_at
)
SELECT gen_random_uuid(),
       p.parent_id,
       glp.level_number,
       glp.item_count,
       glp.time_limit_seconds,
       glp.score_per_correct,
       glp.lives,
       glp.config_json,
       glp.created_at,
       glp.updated_at
FROM game_level_presets glp
JOIN tmp_glp_parent p
  ON p.game_type_id = glp.game_type_id
 AND p.difficulty = glp.difficulty
ON CONFLICT (preset_id, level_number) DO NOTHING;

-- 4) Re-point round_game_configs if they referenced a non-parent legacy preset row.
UPDATE round_game_configs rgc
SET resolved_preset_id = p.parent_id
FROM game_level_presets glp
JOIN tmp_glp_parent p
  ON p.game_type_id = glp.game_type_id
 AND p.difficulty = glp.difficulty
WHERE rgc.resolved_preset_id = glp.id
  AND rgc.resolved_preset_id <> p.parent_id;

-- 5) Remove duplicate legacy rows, keep parent row only.
DELETE FROM game_level_presets glp
USING tmp_glp_parent p
WHERE glp.game_type_id = p.game_type_id
  AND glp.difficulty = p.difficulty
  AND glp.id <> p.parent_id;

DROP TABLE tmp_glp_parent;

-- 6) Replace old uniqueness and drop moved columns from parent table.
ALTER TABLE game_level_presets DROP CONSTRAINT IF EXISTS uk_glp_game_type_difficulty_level;
ALTER TABLE game_level_presets ADD CONSTRAINT uk_glp_game_type_difficulty UNIQUE (game_type_id, difficulty);

ALTER TABLE game_level_presets DROP COLUMN IF EXISTS level_number;
ALTER TABLE game_level_presets DROP COLUMN IF EXISTS item_count;
ALTER TABLE game_level_presets DROP COLUMN IF EXISTS time_limit_seconds;
ALTER TABLE game_level_presets DROP COLUMN IF EXISTS score_per_correct;
ALTER TABLE game_level_presets DROP COLUMN IF EXISTS lives;
ALTER TABLE game_level_presets DROP COLUMN IF EXISTS config_json;

COMMIT;


