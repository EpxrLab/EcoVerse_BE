package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "game_level_presets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_glp_game_type_difficulty_level",
                        columnNames = {"game_type_id", "difficulty", "level_number"})
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
     * Level number within this difficulty band. Starts at 1.
     * Students begin at level 1 and the system auto-increments during gameplay;
     * School/Partnership never configures this directly.
     */
    @Column(name = "level_number", nullable = false)
    private int levelNumber;

    /**
     * Number of waste items presented per session at this level.
     */
    @Column(name = "item_count", nullable = false)
    private int itemCount;

    /**
     * Time limit in seconds for this level. 0 = no limit.
     */
    @Column(name = "time_limit_seconds", nullable = false)
    private int timeLimitSeconds;

    /**
     * Score awarded per correctly classified item.
     * Used for leaderboard ranking in both School and Partnership campaigns.
     */
    @Column(name = "score_per_correct", nullable = false)
    private int scorePerCorrect = 10;

    /**
     * Number of lives. NULL = unlimited.
     */
    @Column(name = "lives")
    private Integer lives;

    /**
     * Game-type-specific extra configuration stored as JSON.
     * Examples:
     *   WASTE_SORTING  → { "categories": ["RECYCLABLE","ORGANIC"], "showHint": false }
     *   SPEED_CLASSIFY → { "spawnIntervalMs": 1500, "penaltyOnMiss": true }
     * Managed by Admin; never modified by School/Partnership.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private Map<String, Object> configJson;
}
