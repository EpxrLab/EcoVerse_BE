package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record StudentGameSessionSummaryResponse(
        UUID sessionId,
        int currentLevel,
        Integer totalItems,
        Integer correctItems,
        Integer incorrectItems,
        BigDecimal accuracyPercentage,
        Integer timeTakenSeconds,
        Boolean isPassed,
        Integer coinAwarded,
        LocalDateTime sessionStart,
        LocalDateTime sessionEnd
) {
}
