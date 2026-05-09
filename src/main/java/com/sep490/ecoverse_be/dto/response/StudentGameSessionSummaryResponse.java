package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public record StudentGameSessionSummaryResponse(
        UUID sessionId,
        UUID presetId,
        String presetName,
        int currentLevel,
        Integer totalItems,
        Integer correctItems,
        Integer incorrectItems,
        BigDecimal accuracyPercentage,
        Integer timeTakenSeconds,
        Boolean isPassed,
        Integer coinAwarded,
        OffsetDateTime sessionStart,
        OffsetDateTime sessionEnd
) {
}
