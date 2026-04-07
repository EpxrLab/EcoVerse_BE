package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

@Builder
public record StudentPresetLevelConfigResponse(
        int levelNumber,
        int itemCount,
        int timeLimitSeconds,
        int scorePerCorrect,
        Integer lives,
        Set<WasteCategory> wasteCategories,
        Map<String, Object> configJson,
        Boolean coinReceived
) {
}

