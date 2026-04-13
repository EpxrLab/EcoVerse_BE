package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record QuizAttemptSummaryResponse(
        UUID attemptId,
        int attemptNumber,
        BigDecimal scorePercentage,
        int timeTakenSeconds,
        boolean isPassed
) {}
