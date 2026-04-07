package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record StudentRoundPresetConfigResponse(
        UUID presetId,
        QuizDifficulty difficulty,
        List<UUID> configuredSubCategoryIds,
        List<StudentPresetLevelConfigResponse> items
) {
}

