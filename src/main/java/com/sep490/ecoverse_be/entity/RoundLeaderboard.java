package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "round_leaderboards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rl_round_student",
                columnNames = {"campaign_round_id", "student_id"}),
        indexes = {
                @Index(name = "idx_round_lb_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_round_lb_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_round_lb_student_id", columnList = "student_id"),
                @Index(name = "idx_round_lb_school_id", columnList = "school_id"),
                @Index(name = "idx_round_lb_overall_rank", columnList = "overall_rank_in_round"),
                @Index(name = "idx_round_lb_school_rank", columnList = "school_rank_in_round"),
                // Composite index for ranking sort: accuracy DESC, avg_time_seconds ASC
                @Index(name = "idx_round_lb_rank_sort", columnList = "combined_accuracy_percentage, avg_time_seconds"),
                @Index(name = "idx_round_lb_is_advanced", columnList = "is_advanced")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoundLeaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    // ── Ranking metrics ───────────────────────────────────────────────────────

    /**
     * Average accuracy across all completed game sessions in this round.
     * = sum(gameSession.accuracyPercentage) / count(gameSessions)
     */
    @Column(name = "game_accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal gameAccuracyPercentage;

    /**
     * Average accuracy across all completed quiz attempts in this round.
     * = sum(quizAttempt.scorePercentage) / count(quizAttempts)
     */
    @Column(name = "quiz_accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal quizAccuracyPercentage;

    /**
     * Primary ranking metric.
     * = (gameAccuracyPercentage + quizAccuracyPercentage) / 2
     *
     * If only game sessions exist (no quiz in round), equals gameAccuracyPercentage.
     * If only quiz exists, equals quizAccuracyPercentage.
     * Updated in realtime as sessions/attempts complete.
     */
    @Column(name = "combined_accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal combinedAccuracyPercentage;

    /**
     * Tiebreaker metric (lower = better).
     * = (avg game timeTakenSeconds + avg quiz timeTakenSeconds) / 2
     * Only used when combinedAccuracyPercentage values are equal.
     */
    @Column(name = "avg_time_seconds", precision = 8, scale = 2)
    private BigDecimal avgTimeSeconds;

    // ── Activity counts ───────────────────────────────────────────────────────

    @Column(name = "games_completed", nullable = false)
    private int gamesCompleted = 0;

    @Column(name = "quizzes_completed", nullable = false)
    private int quizzesCompleted = 0;

    // ── Coin (School campaigns only) ──────────────────────────────────────────

    /**
     * Total coin earned in this round (sum of GameSession.coinAwarded + QuizAttempt.coinsEarned).
     * NULL for PARTNERSHIP campaigns.
     */
    @Column(name = "total_coins_earned")
    private Integer totalCoinsEarned;

    // ── Rank positions ────────────────────────────────────────────────────────

    /** Rank among all students in this round across all schools. */
    @Column(name = "overall_rank_in_round")
    private Integer overallRankInRound;

    /** Rank among students from the same school in this round. */
    @Column(name = "school_rank_in_round")
    private Integer schoolRankInRound;

    @Column(name = "is_advanced", nullable = false)
    private boolean isAdvanced = false;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
