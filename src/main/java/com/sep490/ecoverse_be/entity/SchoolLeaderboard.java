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
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "student_id"}),
        indexes = {
                @Index(name = "idx_school_lb_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_school_lb_student_id", columnList = "student_id"),
                @Index(name = "idx_school_lb_overall_rank", columnList = "overall_rank"),
                @Index(name = "idx_school_lb_school_rank", columnList = "school_rank"),
                @Index(name = "idx_school_lb_accuracy_percentage", columnList = "accuracy_percentage"),
                @Index(name = "idx_school_lb_total_time_seconds", columnList = "total_time_seconds")
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

    @Column(name = "overall_rank")
    private Integer overallRank;

    @Column(name = "school_rank")
    private Integer schoolRank;

    @Column(name = "total_correct_items")
    private Integer totalCorrectItems = 0;

    @Column(name = "total_items")
    private Integer totalItems = 0;

    @Column(name = "accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds = 0;

    @Column(name = "games_completed")
    private Integer gamesCompleted = 0;

    @Column(name = "quizzes_completed")
    private Integer quizzesCompleted = 0;

    @Column(name = "total_coins_earned", precision = 10, scale = 2)
    private BigDecimal totalCoinsEarned = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
