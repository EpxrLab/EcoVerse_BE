package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "school_leaderboards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sl_campaign_student",
                columnNames = {"campaign_id", "student_id"}),
        indexes = {
                @Index(name = "idx_school_lb_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_school_lb_student_id", columnList = "student_id"),
                @Index(name = "idx_school_lb_school_id", columnList = "school_id"),
                @Index(name = "idx_school_lb_overall_rank", columnList = "overall_rank"),
                @Index(name = "idx_school_lb_school_rank", columnList = "school_rank"),
                @Index(name = "idx_school_lb_rank_sort", columnList = "combined_accuracy_percentage, avg_time_seconds")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchoolLeaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    // ── Ranking metrics ───────────────────────────────────────────────────────

    /**
     * Average accuracy across all completed game sessions in this campaign (1 round).
     */
    @Column(name = "game_accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal gameAccuracyPercentage;

    /**
     * Average accuracy across all completed quiz attempts in this campaign.
     */
    @Column(name = "quiz_accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal quizAccuracyPercentage;

    /**
     * Primary ranking metric = (gameAccuracyPercentage + quizAccuracyPercentage) / 2.
     * If only one type exists, equals that type's accuracy.
     */
    @Column(name = "combined_accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal combinedAccuracyPercentage;

    /**
     * Tiebreaker (lower = better).
     * = (avg game timeTakenSeconds + avg quiz timeTakenSeconds) / 2.
     */
    @Column(name = "avg_time_seconds", precision = 8, scale = 2)
    private BigDecimal avgTimeSeconds;

    // ── Activity counts ───────────────────────────────────────────────────────

    @Column(name = "games_completed", nullable = false)
    private int gamesCompleted = 0;

    @Column(name = "quizzes_completed", nullable = false)
    private int quizzesCompleted = 0;

    // ── Coin (School only) ─────────────────────────────────────────────────────

    /** Total coin earned in this campaign. */
    @Column(name = "total_coins_earned")
    private Integer totalCoinsEarned;

    // ── Rank positions ────────────────────────────────────────────────────────

    @Column(name = "overall_rank")
    private Integer overallRank;

    @Column(name = "school_rank")
    private Integer schoolRank;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
