package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    // ── Step 2b: Allowed sub-categories ───────────────────────────────────────

    /**
     * Sub-categories School/Partnership has selected to appear in game sessions.
     *
     * Constraints enforced at service layer:
     *   - Must be a non-empty subset of gameType.supportedSubCategories.
     *   - School/Partnership CANNOT add sub-categories not in the game type's
     *     supported set; they can only restrict the list further.
     *   - If empty at save time, the system defaults to all supportedSubCategories.
     *
     * Join table: round_game_config_sub_categories (round_game_config_id, sub_category_id)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "round_game_config_sub_categories",
            joinColumns = @JoinColumn(name = "round_game_config_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_category_id")
    )
    private List<WasteSubCategory> allowedSubCategories;

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
     * Level-1 GameLevelPreset for (gameType, resolvedDifficulty). Frozen at SCHEDULED.
     * Students start here; the system increments current_level automatically.
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
