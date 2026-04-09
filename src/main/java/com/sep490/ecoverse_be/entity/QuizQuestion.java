package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_questions",
        indexes = {
                @Index(name = "idx_quiz_questions_quiz_id", columnList = "quiz_id"),
                @Index(name = "idx_quiz_questions_is_active", columnList = "is_active"),
                @Index(name = "idx_quiz_questions_question_type", columnList = "question_type")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int questionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @Column(columnDefinition = "text", nullable = false)
    private String questionText;

    @Column(length = 500)
    private String questionImageUrl;

    @Column(nullable = false)
    private boolean isActive = true;
}
