package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "campaign_rewards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"campaign_id", "rank_position"})
}, indexes = {
        @Index(name = "idx_campaign_rewards_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_campaign_rewards_partnership_id", columnList = "partnership_id"),
        @Index(name = "idx_campaign_rewards_rank_position", columnList = "rank_position"),
        @Index(name = "idx_campaign_rewards_status", columnList = "status")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignReward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", nullable = false)
    private Partnership partnership;

    @Column(nullable = false)
    private Integer rankPosition;

    @Column(nullable = false)
    private String rewardName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    private String sponsorName;

    @Enumerated(EnumType.STRING)
    private PartnershipRewardStatus status = PartnershipRewardStatus.PREPARING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
