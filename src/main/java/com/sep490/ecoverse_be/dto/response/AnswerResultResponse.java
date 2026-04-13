package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AnswerResultResponse(
        UUID questionId,
        String questionText,
        UUID selectedAnswerId,
        String selectedAnswerText,
        UUID correctAnswerId,
        String correctAnswerText,
        boolean isCorrect
) {}
