package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_sessions",
        indexes = {
                @Index(name = "idx_gs_campaign_participant_id", columnList = "campaign_participant_id"),
                @Index(name = "idx_gs_round_game_config_id", columnList = "round_game_config_id"),
                @Index(name = "idx_gs_session_start", columnList = "session_start"),
                @Index(name = "idx_gs_is_completed", columnList = "is_completed"),
                @Index(name = "idx_gs_current_level", columnList = "current_level"),
                @Index(name = "idx_gs_accuracy_percentage", columnList = "accuracy_percentage")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ── Who is playing and in which config ────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_participant_id", nullable = false)
    private CampaignParticipant campaignParticipant;

    /**
     * Unified FK covering both SCHOOL and PARTNERSHIP rounds.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_game_config_id", nullable = false)
    private RoundGameConfig roundGameConfig;

    /**
     * The specific preset used for this session.
     * Allows per-preset level tracking (unlock, coin, attempts).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_level_preset_id")
    private GameLevelPreset gameLevelPreset;

    // ── Level progression ─────────────────────────────────────────────────────

    /**
     * Highest level reached during this session. Starts at 1,
     * auto-incremented by the system when score threshold is met.
     */
    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    // ── Timing ────────────────────────────────────────────────────────────────

    @Column(name = "session_start")
    private OffsetDateTime sessionStart;

    @Column(name = "session_end")
    private OffsetDateTime sessionEnd;

    /**
     * Total seconds from sessionStart to sessionEnd.
     * Used as tiebreaker in ranking: shorter time wins when accuracy is equal.
     */
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    // ── Accuracy (primary ranking metric) ────────────────────────────────────

    @Column(name = "total_items", nullable = false)
    private int totalItems = 0;

    @Column(name = "correct_items", nullable = false)
    private int correctItems = 0;

    @Column(name = "incorrect_items", nullable = false)
    private int incorrectItems = 0;

    /**
     * Accuracy percentage = correctItems / totalItems * 100.
     * Primary metric for both ranking (leaderboard) and coin threshold (School).
     * Computed and stored at session completion for query efficiency.
     */
    @Column(name = "accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    // ── Coin (School only) ────────────────────────────────────────────────────

    /**
     * Coin awarded when the session is completed.
     *
     * Resolution at completion:
     *   1. roundGameConfig.coinPerSession != null → use that value
     *   2. else → DefaultCoinConfig.defaultCoin for (gameType, resolvedDifficulty)
     *
     * NULL for PARTNERSHIP campaigns — no coin economy there.
     * Stored as a snapshot so future Admin changes to DefaultCoinConfig
     * do not affect past sessions.
     */
    @Column(name = "coin_awarded")
    private Integer coinAwarded;

    // ── Completion flags ──────────────────────────────────────────────────────

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "is_passed", nullable = false)
    private boolean isPassed = false;

    // ── Preset snapshot ───────────────────────────────────────────────────────

    /**
     * Snapshot of the resolved GameLevelPreset at session-start time.
     * Protects historical integrity if Admin later edits preset rows.
     *
     * Shape:
     * {
     *   "gameTypeCode": "WASTE_SORTING",
     *   "difficulty": "MEDIUM",
     *   "levelNumber": 1,
     *   "itemCount": 15,
     *   "timeLimitSeconds": 90,
     *   "allowedSubCategoryCodes": ["LEAF", "FRUIT", "FOOD"],
     *   "configJson": { ... }
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preset_snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> presetSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // ── Inverse ───────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "gameSession", fetch = FetchType.LAZY)
    private List<GameSessionItem> sessionItems;
}
