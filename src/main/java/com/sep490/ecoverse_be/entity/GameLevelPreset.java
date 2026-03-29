package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "game_level_presets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_glp_game_type_difficulty",
                        columnNames = {"game_type_id", "difficulty"})
        },
        indexes = {
                @Index(name = "idx_glp_game_type_id", columnList = "game_type_id"),
                @Index(name = "idx_glp_difficulty", columnList = "difficulty"),
                @Index(name = "idx_glp_game_type_difficulty", columnList = "game_type_id, difficulty")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameLevelPreset extends BaseEntity {

    /**
     * Game type this preset belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;

    /**
     * Difficulty band (EASY / MEDIUM / HARD).
     * School/Partnership selects a difficulty; the system then uses all presets
     * for that (game_type, difficulty) pair to drive level progression.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private QuizDifficulty difficulty;

    /**
     * Top-level waste categories that can appear in this preset.
     * Admin configures this at preset level instead of game-type level.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_level_preset_waste_categories",
            joinColumns = @JoinColumn(name = "preset_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "waste_category", nullable = false)
    private Set<WasteCategory> wasteCategories;

    /**
     * Ordered level configs within this preset.
     * Each item represents one playable level for the selected difficulty.
     */
    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("levelNumber ASC")
    private List<GameLevelPresetItem> items;
}
