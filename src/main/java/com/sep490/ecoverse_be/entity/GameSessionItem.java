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
@Table(name = "game_session_items", indexes = {
        @Index(name = "idx_game_session_items_game_session_id", columnList = "game_session_id"),
        @Index(name = "idx_game_session_items_waste_item_id", columnList = "waste_item_id")
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

    @Column(nullable = false)
    private Integer presentedOrder;

    @Enumerated(EnumType.STRING)
    private WasteCategory selectedCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteCategory correctCategory;

    private Boolean isCorrect;

    private Integer timeTakenSeconds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
