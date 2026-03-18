package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CreateQuizRequest;
import com.sep490.ecoverse_be.dto.request.QuizQuestionRequest;
import com.sep490.ecoverse_be.dto.request.UpdateQuizRequest;
import com.sep490.ecoverse_be.dto.response.ImportResultResponse;
import com.sep490.ecoverse_be.dto.response.QuizResponse;
import com.sep490.ecoverse_be.dto.response.QuizSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IQuizService {

    QuizResponse createQuizManual(CreateQuizRequest request);

    ImportResultResponse importQuizFromExcel(MultipartFile file);

    List<QuizSummaryResponse> getMyQuizzes();

    QuizResponse getQuizById(UUID quizId);

    QuizResponse updateQuiz(UUID quizId, UpdateQuizRequest request);

    void deleteQuiz(UUID quizId);

    QuizResponse togglePublish(UUID quizId);

    QuizResponse addQuestions(UUID quizId, List<QuizQuestionRequest> questions);

    QuizResponse updateQuestion(UUID quizId, UUID questionId, QuizQuestionRequest request);

    void deleteQuestion(UUID quizId, UUID questionId);
}
