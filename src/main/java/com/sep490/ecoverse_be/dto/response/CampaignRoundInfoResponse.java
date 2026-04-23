package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RoundStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record CampaignRoundInfoResponse(
        UUID id,
        Integer roundNumber,
        String roundName,
        RoundStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer maxParticipants,
        Integer advanceCount,
        Boolean isFinalRound,
        // Game config
        UUID gameTypeId,
        String gameTypeName,
        Integer coinPerSession,
        List<UUID> selectedPresetIds,
        Map<String, List<UUID>> presetSubCategoryConfig,
        List<StudentRoundGameConfigResponse> games,
        // Quiz config
        List<RoundQuizBriefResponse> quizzes
) {
}
