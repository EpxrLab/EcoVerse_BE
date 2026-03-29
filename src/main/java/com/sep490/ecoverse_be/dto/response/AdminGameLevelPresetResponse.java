package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Builder
public record AdminGameLevelPresetResponse(
        UUID id,
        UUID gameTypeId,
        QuizDifficulty difficulty,
        Set<WasteCategory> wasteCategories,
        List<AdminGameLevelPresetItemResponse> items
) {
}

