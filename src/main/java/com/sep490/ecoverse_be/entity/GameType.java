package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.GameTypeCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "game_types",
        indexes = {
                @Index(name = "idx_game_types_type_code", columnList = "type_code"),
                @Index(name = "idx_game_types_is_active", columnList = "is_active"),
                @Index(name = "idx_game_types_display_order", columnList = "display_order")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameType extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false)
    private GameTypeCode typeCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "short_description", length = 500, nullable = false)
    private String shortDescription;

    @Column(name = "full_description", columnDefinition = "text")
    private String fullDescription;

    @Column(name = "how_to_play", columnDefinition = "text", nullable = false)
    private String howToPlay;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;


    /**
     * Admin defines which WasteSubCategories are available for this game type.
     * School/Partnership can then choose a subset of these when configuring a round.
     *
     * Join table: game_type_sub_categories (game_type_id, sub_category_id)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "game_type_sub_categories",
            joinColumns = @JoinColumn(name = "game_type_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_category_id")
    )
    private List<WasteSubCategory> supportedSubCategories;

    /**
     * Feature flags specific to this game type.
     * e.g. { "hasStreakBonus": true, "showHintButton": false }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> features;

    /**
     * Whether this game type participates in the coin economy.
     * true  = School campaigns award coin_per_session on completion.
     * false = score-only; Partnership-only game types.
     */
    @Column(name = "supports_coin", nullable = false)
    private boolean supportsCoin = true;

    /**
     * Maximum level_number across all GameLevelPresets for this game type.
     */
    @Column(name = "max_levels", nullable = false)
    private int maxLevels;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_delete", nullable = false)
    private boolean isDelete = false;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    // ── Inverse relations ─────────────────────────────────────────────────────

    @OneToMany(mappedBy = "gameType", fetch = FetchType.LAZY)
    private List<GameLevelPreset> levelPresets;

    @OneToMany(mappedBy = "gameType", fetch = FetchType.LAZY)
    private List<DifficultyRecommendation> difficultyRecommendations;

    @OneToMany(mappedBy = "gameType", fetch = FetchType.LAZY)
    private List<DefaultCoinConfig> defaultCoinConfigs;
}
