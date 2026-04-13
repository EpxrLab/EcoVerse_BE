package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "default_coin_configs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dcc_game_type_difficulty",
                        columnNames = {"game_type_id", "difficulty"})
        },
        indexes = {
                @Index(name = "idx_dcc_game_type_id", columnList = "game_type_id")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DefaultCoinConfig extends BaseEntity {

    /**
     * Game type this default applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;

    /**
     * Difficulty this default applies to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private QuizDifficulty difficulty;

    /**
     * Coin awarded per completed session at this (game_type, difficulty) combination.
     * Used when School does not set a custom coin_per_session in RoundGameConfig.
     * Not applicable to Partnership campaigns (coin_awarded stays null there).
     */
    @Column(name = "default_coin", nullable = false)
    private int defaultCoin;
}
