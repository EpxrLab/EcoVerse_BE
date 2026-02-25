package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.QuizSource;
import com.sep490.ecoverse_be.enums.QuizType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quizzes", indexes = {
        @Index(name = "idx_quizzes_school_id", columnList = "school_id"),
        @Index(name = "idx_quizzes_difficulty", columnList = "difficulty"),
        @Index(name = "idx_quizzes_quiz_type", columnList = "quiz_type"),
        @Index(name = "idx_quizzes_source", columnList = "source"),
        @Index(name = "idx_quizzes_is_published", columnList = "is_published")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Quiz extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType quizType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizSource source;

    @Column(length = 500)
    private String sourceDocumentUrl;

    @Column(nullable = false)
    private Integer pointsReward = 10;

    private Integer timePerQuestion;

    @Column(nullable = false)
    private Integer passScorePercentage = 70;

    @Column(nullable = false)
    private boolean isPublished = false;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
