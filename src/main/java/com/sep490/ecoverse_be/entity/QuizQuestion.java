package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_quiz_questions_quiz_order", columnNames = {"quiz_id", "question_order"})
        },
        indexes = {
                @Index(name = "idx_quiz_questions_quiz_id", columnList = "quiz_id")
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

    @Column(columnDefinition = "text", nullable = false)
    private String questionText;

    @Column(length = 500)
    private String questionImageUrl;
}
