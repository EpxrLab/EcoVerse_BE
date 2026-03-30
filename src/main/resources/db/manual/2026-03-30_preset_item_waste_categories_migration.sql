-- Manual PostgreSQL migration for moving waste categories from preset-level to preset-item-level.
-- Run after backing up data.

BEGIN;

CREATE TABLE IF NOT EXISTS game_level_preset_item_waste_categories (
    preset_item_id uuid NOT NULL,
    waste_category varchar(50) NOT NULL,
    CONSTRAINT fk_glpiwc_item FOREIGN KEY (preset_item_id) REFERENCES game_level_preset_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_glpiwc_item_category UNIQUE (preset_item_id, waste_category)
);

CREATE INDEX IF NOT EXISTS idx_glpiwc_item_id ON game_level_preset_item_waste_categories(preset_item_id);

-- Backfill item-level categories from existing preset-level categories when legacy table exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'game_level_preset_waste_categories'
    ) THEN
        INSERT INTO game_level_preset_item_waste_categories (preset_item_id, waste_category)
        SELECT glpi.id, glpwc.waste_category
        FROM game_level_preset_items glpi
        JOIN game_level_preset_waste_categories glpwc ON glpwc.preset_id = glpi.preset_id
        ON CONFLICT (preset_item_id, waste_category) DO NOTHING;
    END IF;
END $$;

COMMIT;


