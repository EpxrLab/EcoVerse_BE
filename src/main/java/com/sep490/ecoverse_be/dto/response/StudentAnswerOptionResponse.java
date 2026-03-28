package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record StudentAnswerOptionResponse(
        UUID answerId,
        String answerText
) {}
