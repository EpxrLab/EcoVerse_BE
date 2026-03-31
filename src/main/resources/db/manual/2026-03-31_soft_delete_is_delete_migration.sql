-- Manual PostgreSQL migration to standardize soft delete via is_delete.
-- Run after backing up data.

BEGIN;

ALTER TABLE game_types
    ADD COLUMN IF NOT EXISTS is_delete boolean NOT NULL DEFAULT false;

ALTER TABLE game_types
    DROP CONSTRAINT IF EXISTS game_types_type_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_game_types_type_code_not_deleted
    ON game_types(type_code)
    WHERE is_delete = false;

ALTER TABLE waste_sub_categories
    ADD COLUMN IF NOT EXISTS is_delete boolean NOT NULL DEFAULT false;

ALTER TABLE waste_sub_categories
    DROP CONSTRAINT IF EXISTS uk_wsc_category_code;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wsc_category_code_not_deleted
    ON waste_sub_categories(category, sub_category_code)
    WHERE is_delete = false;

ALTER TABLE waste_items
    ADD COLUMN IF NOT EXISTS is_delete boolean NOT NULL DEFAULT false;

ALTER TABLE rewards
    ADD COLUMN IF NOT EXISTS is_delete boolean NOT NULL DEFAULT false;

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS is_delete boolean NOT NULL DEFAULT false;

ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS is_delete boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_game_types_is_delete ON game_types(is_delete);
CREATE INDEX IF NOT EXISTS idx_waste_sub_categories_is_delete ON waste_sub_categories(is_delete);
CREATE INDEX IF NOT EXISTS idx_waste_items_is_delete ON waste_items(is_delete);
CREATE INDEX IF NOT EXISTS idx_rewards_is_delete ON rewards(is_delete);
CREATE INDEX IF NOT EXISTS idx_quizzes_is_delete ON quizzes(is_delete);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_is_delete ON quiz_questions(is_delete);

COMMIT;



