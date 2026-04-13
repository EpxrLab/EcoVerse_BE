package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "game_session_items",
        indexes = {
                @Index(name = "idx_gsi_game_session_id", columnList = "game_session_id"),
                @Index(name = "idx_gsi_waste_item_id", columnList = "waste_item_id"),
                @Index(name = "idx_gsi_level_number", columnList = "level_number"),
                @Index(name = "idx_gsi_is_correct", columnList = "is_correct")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waste_item_id", nullable = false)
    private WasteItem wasteItem;

    /**
     * Level at which this item was presented.
     * Allows per-level accuracy analysis without a separate query to GameSession.
     */
    @Column(name = "level_number", nullable = false)
    private int levelNumber;

    @Column(name = "presented_order", nullable = false)
    private int presentedOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_category")
    private WasteCategory selectedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_category", nullable = false)
    private WasteCategory correctCategory;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
