package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public record AdminGameLevelPresetItemResponse(
        UUID id,
        int levelNumber,
        int itemCount,
        int timeLimitSeconds,
        int scorePerCorrect,
        Integer lives,
        Map<String, Object> configJson
) {
}

