package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.GameTypeCode;
import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "game_type_difficulty_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_type", "difficulty"}),
        indexes = {
                @Index(name = "idx_gtdc_game_type", columnList = "game_type"),
                @Index(name = "idx_gtdc_difficulty", columnList = "difficulty"),
                @Index(name = "idx_gtdc_is_active", columnList = "is_active")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameTypeDifficultyConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameTypeCode gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private QuizDifficulty difficulty;

    @Column(name = "config_name", length = 255, nullable = false)
    private String configName;

    @Column(name = "total_levels", nullable = false)
    private Integer totalLevels;

    @Column(name = "base_capacity")
    private Integer baseCapacity;

    @Column(name = "base_lives")
    private Integer baseLives;

    @Column(name = "base_time_limit_seconds", nullable = false)
    private Integer baseTimeLimitSeconds;

    @Column(name = "coins_per_level", precision = 10, scale = 2)
    private BigDecimal coinsPerLevel = BigDecimal.ZERO;

    @Column(name = "coins_bonus_completion", precision = 10, scale = 2)
    private BigDecimal coinsBonusCompletion = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
