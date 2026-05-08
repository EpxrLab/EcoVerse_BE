package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RoundQuizBriefResponse(
        UUID quizId,
        String title,
        QuizDifficulty difficulty,
        int displayOrder,
        int attemptsUsed,
        boolean isPassed,
        int maxAttempts,
        boolean isRequired,
        Integer coinsOnPass
) {
}
