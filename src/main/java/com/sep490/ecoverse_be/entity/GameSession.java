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
@Table(name = "game_sessions", indexes = {
        @Index(name = "idx_game_sessions_campaign_participant_id", columnList = "campaign_participant_id"),
        @Index(name = "idx_game_sessions_campaign_game_id", columnList = "campaign_game_id"),
        @Index(name = "idx_game_sessions_campaign_round_game_id", columnList = "campaign_round_game_id"),
        @Index(name = "idx_game_sessions_game_level_config_id", columnList = "game_level_config_id"),
        @Index(name = "idx_game_sessions_session_start", columnList = "session_start"),
        @Index(name = "idx_game_sessions_is_completed", columnList = "is_completed"),
        @Index(name = "idx_game_sessions_accuracy_percentage", columnList = "accuracy_percentage"),
        @Index(name = "idx_game_sessions_time_taken_seconds", columnList = "time_taken_seconds")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_participant_id", nullable = false)
    private CampaignParticipant campaignParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_game_id")
    private CampaignGame campaignGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_game_id")
    private CampaignRoundGame campaignRoundGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_level_config_id", nullable = false)
    private GameLevelConfig gameLevelConfig;

    private LocalDateTime sessionStart;

    private LocalDateTime sessionEnd;

    private Integer totalItems = 0;

    private Integer correctItems = 0;

    private Integer incorrectItems = 0;

    @Column(precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    private Integer timeTakenSeconds;

    @Column(precision = 10, scale = 2)
    private BigDecimal coinsEarned = BigDecimal.ZERO;

    private Boolean isCompleted = false;

    private Boolean isPassed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
