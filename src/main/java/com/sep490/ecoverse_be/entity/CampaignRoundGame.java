package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_round_games",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_round_id", "display_order"}),
        indexes = {
                @Index(name = "idx_campaign_round_games_campaign_round_id", columnList = "campaign_round_id"),
                @Index(name = "idx_campaign_round_games_game_type_config_id", columnList = "game_type_config_id")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignRoundGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_config_id", nullable = false)
    private GameTypeDifficultyConfig gameTypeDifficultyConfig;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_required")
    private Boolean isRequired = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
