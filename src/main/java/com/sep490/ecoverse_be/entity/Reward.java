package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.RewardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "rewards", indexes = {
        @Index(name = "idx_rewards_school_id", columnList = "school_id"),
        @Index(name = "idx_rewards_reward_type", columnList = "reward_type"),
        @Index(name = "idx_rewards_is_active", columnList = "is_active"),
        @Index(name = "idx_rewards_coin_cost", columnList = "coin_cost")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Reward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false)
    private String rewardName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType rewardType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal coinCost;

    @Column(length = 500)
    private String imageUrl;

    private Integer stockQuantity;

    private Boolean isUnlimited = false;

    private Boolean isActive = true;

    @Column(columnDefinition = "text")
    private String termsConditions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
