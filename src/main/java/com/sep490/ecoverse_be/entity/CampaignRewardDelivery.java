package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_reward_deliveries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"campaign_reward_id", "student_id"})
}, indexes = {
        @Index(name = "idx_crd_campaign_reward_id", columnList = "campaign_reward_id"),
        @Index(name = "idx_crd_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_crd_campaign_round_id", columnList = "campaign_round_id"),
        @Index(name = "idx_crd_round_leaderboard_id", columnList = "round_leaderboard_id"),
        @Index(name = "idx_crd_student_id", columnList = "student_id"),
        @Index(name = "idx_crd_school_id", columnList = "school_id"),
        @Index(name = "idx_crd_status", columnList = "status"),
        @Index(name = "idx_crd_leaderboard_rank", columnList = "leaderboard_rank")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignRewardDelivery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_reward_id", nullable = false)
    private CampaignReward campaignReward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_leaderboard_id", nullable = false)
    private RoundLeaderboard roundLeaderboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false)
    private Integer leaderboardRank;

    @Enumerated(EnumType.STRING)
    private PartnershipRewardStatus status = PartnershipRewardStatus.PREPARING;

    private LocalDateTime preparingAt;

    private LocalDateTime shippedAt;

    @Column(length = 100)
    private String shippingTrackingCode;

    private LocalDateTime arrivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrived_confirmed_by")
    private User arrivedConfirmedBy;

    private LocalDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivered_by")
    private User deliveredBy;

    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Parent confirmedBy;

    @Column(columnDefinition = "text")
    private String notes;
}
