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
@Table(name = "round_leaderboards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_round_id", "student_id"}),
        indexes = {
                @Index(name = "idx_round_lb_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_round_lb_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_round_lb_student_id", columnList = "student_id"),
                @Index(name = "idx_round_lb_overall_rank_in_round", columnList = "overall_rank_in_round"),
                @Index(name = "idx_round_lb_school_rank_in_round", columnList = "school_rank_in_round"),
                @Index(name = "idx_round_lb_accuracy_percentage", columnList = "accuracy_percentage"),
                @Index(name = "idx_round_lb_total_time_seconds", columnList = "total_time_seconds"),
                @Index(name = "idx_round_lb_is_advanced", columnList = "is_advanced"),
                @Index(name = "idx_round_lb_campaign_student", columnList = "campaign_id, student_id")
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

    @Column(name = "overall_rank_in_round")
    private Integer overallRankInRound;

    @Column(name = "school_rank_in_round")
    private Integer schoolRankInRound;

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

    @Column(name = "is_advanced")
    private Boolean isAdvanced = false;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
