package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"campaign_participant_id", "quiz_id", "attempt_number"})
}, indexes = {
        @Index(name = "idx_quiz_attempts_campaign_participant_id", columnList = "campaign_participant_id"),
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
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    private Integer attemptNumber = 1;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer totalQuestions;

    private Integer correctAnswers = 0;

    @Column(precision = 5, scale = 2)
    private BigDecimal scorePercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal coinsEarned = BigDecimal.ZERO;

    private Integer timeTakenSeconds;

    private Boolean isPassed = false;

    private Boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
