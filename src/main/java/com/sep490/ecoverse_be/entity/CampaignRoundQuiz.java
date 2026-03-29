package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campaign_round_quizzes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crq_round_quiz",
                        columnNames = {"campaign_round_id", "quiz_id"})
        },
        indexes = {
                @Index(name = "idx_crq_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_crq_quiz_id", columnList = "quiz_id"),
                @Index(name = "idx_crq_display_order", columnList = "campaign_round_id, display_order")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignRoundQuiz extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    // So lan lam lai toi da, configurable boi school/partnership (mac dinh 3)
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    // Student phai hoan thanh quiz nay moi duoc xep hang leaderboard
    @Column(name = "is_required", nullable = false)
    private boolean isRequired = true;
}
