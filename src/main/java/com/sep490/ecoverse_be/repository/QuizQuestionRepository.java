package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByQuizIdAndIsActiveTrueOrderByQuestionOrder(UUID quizId);

    Optional<QuizQuestion> findByIdAndQuizIdAndIsActiveTrue(UUID id, UUID quizId);

    boolean existsByQuizIdAndQuestionOrderAndIsActiveTrue(UUID quizId, int questionOrder);

    int countByQuizIdAndIsActiveTrue(UUID quizId);

    List<QuizQuestion> findByQuizId(UUID quizId);
}
