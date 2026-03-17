package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.QuizAnswer;
import com.sep490.ecoverse_be.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {


    List<QuizAnswer> findByQuestionIn(List<QuizQuestion> questions);

    void deleteAllByQuestionId(UUID questionId);
}
