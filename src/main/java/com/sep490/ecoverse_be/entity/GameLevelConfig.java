package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "game_level_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_type_config_id", "level_number"}),
        indexes = {
                @Index(name = "idx_glc_game_type_config_id", columnList = "game_type_config_id"),
                @Index(name = "idx_glc_is_active", columnList = "is_active")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameLevelConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_config_id", nullable = false)
    private GameTypeDifficultyConfig gameTypeDifficultyConfig;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    @Column(name = "level_name", length = 255, nullable = false)
    private String levelName;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "lives_count")
    private Integer livesCount;

    @Column(name = "items_count", nullable = false)
    private Integer itemsCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_waste_categories", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> allowedWasteCategories;

    @Column(name = "capacity")
    private Integer capacity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "obstacle_config", columnDefinition = "jsonb")
    private Map<String, Object> obstacleConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_items_config", columnDefinition = "jsonb")
    private Map<String, Object> specialItemsConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "waste_item_pool", columnDefinition = "jsonb")
    private Map<String, Object> wasteItemPool;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unlock_condition", columnDefinition = "jsonb")
    private Map<String, Object> unlockCondition;

    @Column(name = "coins_reward", precision = 10, scale = 2)
    private BigDecimal coinsReward = BigDecimal.ZERO;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
