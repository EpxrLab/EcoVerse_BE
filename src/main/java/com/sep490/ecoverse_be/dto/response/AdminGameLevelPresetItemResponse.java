package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
public record AdminGameLevelPresetItemResponse(
        UUID id,
        int levelNumber,
        int itemCount,
        int timeLimitSeconds,
        int scorePerCorrect,
        Integer lives,
        Set<WasteCategory> wasteCategories,
        Map<String, Object> configJson
) {
}


