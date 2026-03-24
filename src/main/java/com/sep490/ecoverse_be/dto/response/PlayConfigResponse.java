package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record PlayConfigResponse(
        UUID campaignId,
        UUID roundId,
        UUID gameTypeId,
        String gameTypeName,
        QuizDifficulty resolvedDifficulty,
        Integer coinPerSession,
        UUID quizId,
        List<UUID> selectedSubCategoryIds
) {
}

