package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(UUID quizId);

    Optional<QuizQuestion> findByIdAndQuizIdAndIsDeleteFalse(UUID id, UUID quizId);

    boolean existsByQuizIdAndQuestionOrderAndIsDeleteFalse(UUID quizId, int questionOrder);

    int countByQuizIdAndIsDeleteFalse(UUID quizId);

    List<QuizQuestion> findByQuizId(UUID quizId);
}
