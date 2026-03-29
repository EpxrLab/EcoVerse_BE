package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record QuizAttemptResultResponse(
        UUID attemptId,
        int attemptNumber,
        int correctAnswers,
        int totalQuestions,
        BigDecimal scorePercentage,
        int timeTakenSeconds,
        boolean isPassed,
        Integer coinsEarned,
        // Diem cao nhat trong tat ca lan lam (best of N)
        BigDecimal bestScorePercentage,
        int attemptsUsed,
        int maxAttempts,
        // Ket qua chi tiet tung cau hoi
        List<AnswerResultResponse> answerResults
) {}
