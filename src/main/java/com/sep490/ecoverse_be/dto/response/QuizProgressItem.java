package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record QuizProgressItem(
        UUID quizId,
        String quizTitle,
        int displayOrder,
        int maxAttempts,
        int attemptsUsed,
        BigDecimal bestScore,
        boolean isPassed,
        // NOT_STARTED / IN_PROGRESS / PASSED / FAILED
        String status
) {}
