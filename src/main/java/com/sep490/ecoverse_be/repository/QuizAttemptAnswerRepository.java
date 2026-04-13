package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, UUID> {

    // Lay tat ca cau tra loi cua 1 attempt
    List<QuizAttemptAnswer> findByQuizAttemptId(UUID attemptId);

    // Dem so cau tra loi cua 1 attempt
    int countByQuizAttemptId(UUID attemptId);
}
