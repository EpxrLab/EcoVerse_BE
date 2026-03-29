package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "game_level_preset_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_glpi_preset_level",
                        columnNames = {"preset_id", "level_number"})
        },
        indexes = {
                @Index(name = "idx_glpi_preset_id", columnList = "preset_id"),
                @Index(name = "idx_glpi_level_number", columnList = "level_number")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameLevelPresetItem extends BaseEntity {

    /**
     * Parent preset that groups all levels for one (gameType, difficulty) pair.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private GameLevelPreset preset;

    /**
     * Level number within this difficulty band. Starts at 1.
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
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private Map<String, Object> configJson;
}

