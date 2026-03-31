package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "round_game_configs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rgc_round_order",
                        columnNames = {"campaign_round_id", "display_order"})
        },
        indexes = {
                @Index(name = "idx_rgc_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_rgc_game_type_id", columnList = "game_type_id"),
                @Index(name = "idx_rgc_resolved_preset_id", columnList = "resolved_preset_id")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoundGameConfig extends BaseEntity {

    // ── Campaign round ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    // ── Step 2: School/Partnership selects game type ───────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;

    // ── Step 2b: Selected presets ──────────────────────────────────────────────

    /**
     * Presets selected by School/Partnership for this round.
     * Must be one or more presets belonging to the selected game type.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "round_game_config_presets",
            joinColumns = @JoinColumn(name = "round_game_config_id"),
            inverseJoinColumns = @JoinColumn(name = "preset_id")
    )
    private List<GameLevelPreset> selectedPresets;

    /**
     * Per-preset selectable sub-category IDs constrained by that preset's item-level wasteCategories.
     * Key = presetId (string), value = selected sub-category IDs.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preset_sub_category_config", columnDefinition = "jsonb")
    private Map<String, List<UUID>> presetSubCategoryConfig;

    // ── Step 3: Difficulty (optional override) ─────────────────────────────────

    /**
     * Difficulty override set by School/Partnership.
     * NULL = system auto-selects from DifficultyRecommendation
     * based on the campaign's student grade range.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_override")
    private QuizDifficulty difficultyOverride;

    // ── System-resolved (frozen at SCHEDULED) ─────────────────────────────────

    /**
     * Final difficulty actually used. Frozen when campaign moves to SCHEDULED.
     * = difficultyOverride if set, else DifficultyRecommendation.suggestedDifficulty.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_difficulty")
    private QuizDifficulty resolvedDifficulty;

    /**
     * Resolved preset for (gameType, resolvedDifficulty). Frozen at SCHEDULED.
     * Runtime uses preset.items ordered by levelNumber for level progression.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_preset_id")
    private GameLevelPreset resolvedPreset;

    // ── Step 1: Coin config (School campaigns only) ────────────────────────────

    /**
     * Custom coin per completed session set by School.
     * NULL → use DefaultCoinConfig.defaultCoin for (gameType, resolvedDifficulty).
     * Must be NULL for PARTNERSHIP campaigns.
     */
    @Column(name = "coin_per_session")
    private Integer coinPerSession;

    // ── Display ───────────────────────────────────────────────────────────────

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
