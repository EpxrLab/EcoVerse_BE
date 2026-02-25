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
@Table(name = "campaign_games",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_games_campaign_config", columnNames = {"campaign_id", "game_type_config_id"})
        },
        indexes = {
                @Index(name = "idx_campaign_games_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_campaign_games_game_type_config_id", columnList = "game_type_config_id"),
                @Index(name = "idx_campaign_games_display_order", columnList = "display_order")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_config_id", nullable = false)
    private GameTypeDifficultyConfig gameTypeConfig;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
