package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
public record StudentGameSessionStartResponse(
        UUID sessionId,
        UUID campaignId,
        UUID roundId,
        UUID roundGameConfigId,
        UUID gameTypeId,
        String gameTypeName,
        QuizDifficulty resolvedDifficulty,
        int levelNumber,
        int itemCount,
        int timeLimitSeconds,
        int scorePerCorrect,
        Integer lives,
        Set<WasteCategory> wasteCategories,
        LocalDateTime sessionStart,
        Map<String, Object> presetSnapshot,
        List<GameLevelWasteItemResponse> wasteItems
) {
}
