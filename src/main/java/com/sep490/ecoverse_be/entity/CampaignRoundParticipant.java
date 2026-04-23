package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "campaign_round_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_round_id", "campaign_participant_id"}),
        indexes = {
                @Index(name = "idx_crp_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_crp_campaign_participant_id", columnList = "campaign_participant_id"),
                @Index(name = "idx_crp_rank_in_round", columnList = "rank_in_round"),
                @Index(name = "idx_crp_is_advanced", columnList = "is_advanced"),
                @Index(name = "idx_crp_accuracy_percentage", columnList = "accuracy_percentage"),
                @Index(name = "idx_crp_total_time_seconds", columnList = "total_time_seconds")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignRoundParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_participant_id", nullable = false)
    private CampaignParticipant campaignParticipant;

    @Column(name = "total_correct_items")
    private Integer totalCorrectItems = 0;

    @Column(name = "total_items")
    private Integer totalItems = 0;

    @Column(name = "accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds = 0;

    @Column(name = "rank_in_round")
    private Integer rankInRound;

    @Column(name = "is_advanced")
    private Boolean isAdvanced = false;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
