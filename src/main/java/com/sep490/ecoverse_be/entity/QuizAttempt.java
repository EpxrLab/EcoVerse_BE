package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_qa_participant_round_quiz_attempt",
                        columnNames = {"campaign_participant_id", "campaign_round_id", "quiz_id", "attempt_number"})
        },
        indexes = {
                @Index(name = "idx_quiz_attempts_campaign_participant_id", columnList = "campaign_participant_id"),
                @Index(name = "idx_quiz_attempts_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_quiz_attempts_quiz_id", columnList = "quiz_id"),
                @Index(name = "idx_quiz_attempts_is_completed", columnList = "is_completed"),
                @Index(name = "idx_quiz_attempts_score_percentage", columnList = "score_percentage")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_participant_id", nullable = false)
    private CampaignParticipant campaignParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers = 0;

    /**
     * Primary ranking metric for quiz component.
     * = correctAnswers / totalQuestions * 100
     * Combined with gameSession.accuracyPercentage in leaderboard calculations.
     */
    @Column(name = "score_percentage", precision = 5, scale = 2)
    private BigDecimal scorePercentage;

    /**
     * Time taken in seconds.
     * Used as tiebreaker in leaderboard when combined accuracy is equal.
     */
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    /**
     * Coin awarded on quiz completion (School campaigns only).
     * NULL for Partnership campaigns.
     * Based on passScorePercentage threshold defined in Quiz.
     */
    @Column(name = "coins_earned")
    private Integer coinsEarned;

    @Column(name = "is_passed", nullable = false)
    private boolean isPassed = false;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
