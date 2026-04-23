package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempt_answers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"quiz_attempt_id", "question_id"})
}, indexes = {
        @Index(name = "idx_quiz_attempt_answers_quiz_attempt_id", columnList = "quiz_attempt_id"),
        @Index(name = "idx_quiz_attempt_answers_question_id", columnList = "question_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_answer_id")
    private QuizAnswer selectedAnswer;

    private Boolean isCorrect;

    private Integer timeTakenSeconds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
