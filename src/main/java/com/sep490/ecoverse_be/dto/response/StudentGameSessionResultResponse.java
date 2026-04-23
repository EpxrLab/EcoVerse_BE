package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public record StudentGameSessionResultResponse(
        UUID sessionId,
        UUID campaignId,
        UUID roundId,
        UUID roundGameConfigId,
        int currentLevel,
        Integer totalItems,
        Integer correctItems,
        Integer incorrectItems,
        BigDecimal accuracyPercentage,
        Integer timeTakenSeconds,
        Boolean isPassed,
        Integer coinAwarded,
        String feedbackMessage,
        Boolean isCompleted,
        OffsetDateTime sessionStart,
        OffsetDateTime sessionEnd
) {
}
