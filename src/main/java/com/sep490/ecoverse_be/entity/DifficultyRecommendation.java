package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "difficulty_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dr_game_type_grade_range",
                        columnNames = {"game_type_id", "grade_min", "grade_max"})
        },
        indexes = {
                @Index(name = "idx_dr_game_type_id", columnList = "game_type_id")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DifficultyRecommendation extends BaseEntity {

    /**
     * Game type this recommendation applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;

    /**
     * Inclusive lower bound of the grade range (e.g. 1 for Grade 1).
     */
    @Column(name = "grade_min", nullable = false)
    private int gradeMin;

    /**
     * Inclusive upper bound of the grade range (e.g. 3 for Grade 3).
     */
    @Column(name = "grade_max", nullable = false)
    private int gradeMax;

    /**
     * Difficulty the system will pre-select in the wizard when the campaign's
     * student grade range falls within [gradeMin, gradeMax].
     * School/Partnership can still override this choice.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_difficulty", nullable = false)
    private QuizDifficulty suggestedDifficulty;

    /**
     * Starting level within the suggested difficulty.
     * Almost always 1; exposed for edge cases where Admin wants to skip early levels.
     */
    @Column(name = "suggested_level_start", nullable = false)
    private int suggestedLevelStart = 1;
}
