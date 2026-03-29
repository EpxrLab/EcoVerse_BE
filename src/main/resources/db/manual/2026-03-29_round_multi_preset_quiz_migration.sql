-- Manual PostgreSQL migration for multi-preset round config and multi-quiz round binding.
-- Run after backing up data.

BEGIN;

-- 1) Preset-level waste categories
CREATE TABLE IF NOT EXISTS game_level_preset_waste_categories (
    preset_id uuid NOT NULL,
    waste_category varchar(50) NOT NULL,
    CONSTRAINT fk_glpwc_preset FOREIGN KEY (preset_id) REFERENCES game_level_presets(id) ON DELETE CASCADE,
    CONSTRAINT uk_glpwc_preset_category UNIQUE (preset_id, waste_category)
);

CREATE INDEX IF NOT EXISTS idx_glpwc_preset_id ON game_level_preset_waste_categories(preset_id);

-- Backfill from legacy game_type_sub_categories if table exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'game_type_sub_categories'
    ) THEN
        INSERT INTO game_level_preset_waste_categories (preset_id, waste_category)
        SELECT DISTINCT glp.id, wsc.category::varchar
        FROM game_level_presets glp
        JOIN game_type_sub_categories gtsc ON gtsc.game_type_id = glp.game_type_id
        JOIN waste_sub_categories wsc ON wsc.id = gtsc.sub_category_id
        ON CONFLICT (preset_id, waste_category) DO NOTHING;
    END IF;
END $$;

-- 2) Round config can select many presets.
CREATE TABLE IF NOT EXISTS round_game_config_presets (
    round_game_config_id uuid NOT NULL,
    preset_id uuid NOT NULL,
    CONSTRAINT fk_rgcp_config FOREIGN KEY (round_game_config_id) REFERENCES round_game_configs(id) ON DELETE CASCADE,
    CONSTRAINT fk_rgcp_preset FOREIGN KEY (preset_id) REFERENCES game_level_presets(id),
    CONSTRAINT uk_rgcp UNIQUE (round_game_config_id, preset_id)
);

CREATE INDEX IF NOT EXISTS idx_rgcp_config_id ON round_game_config_presets(round_game_config_id);
CREATE INDEX IF NOT EXISTS idx_rgcp_preset_id ON round_game_config_presets(preset_id);

ALTER TABLE round_game_configs
    ADD COLUMN IF NOT EXISTS preset_sub_category_config jsonb;

-- Backfill from resolved_preset_id where available.
INSERT INTO round_game_config_presets (round_game_config_id, preset_id)
SELECT id, resolved_preset_id
FROM round_game_configs
WHERE resolved_preset_id IS NOT NULL
ON CONFLICT (round_game_config_id, preset_id) DO NOTHING;

-- 3) Round can bind many quizzes.
CREATE TABLE IF NOT EXISTS campaign_round_quizzes (
    campaign_round_id uuid NOT NULL,
    quiz_id uuid NOT NULL,
    CONSTRAINT fk_crq_round FOREIGN KEY (campaign_round_id) REFERENCES campaign_rounds(id) ON DELETE CASCADE,
    CONSTRAINT fk_crq_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
    CONSTRAINT uk_crq UNIQUE (campaign_round_id, quiz_id)
);

CREATE INDEX IF NOT EXISTS idx_crq_round_id ON campaign_round_quizzes(campaign_round_id);
CREATE INDEX IF NOT EXISTS idx_crq_quiz_id ON campaign_round_quizzes(quiz_id);

-- Backfill legacy single quiz relation.
INSERT INTO campaign_round_quizzes (campaign_round_id, quiz_id)
SELECT id, quiz_id
FROM campaign_rounds
WHERE quiz_id IS NOT NULL
ON CONFLICT (campaign_round_id, quiz_id) DO NOTHING;

COMMIT;


