package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RoundStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record CampaignRoundInfoResponse(
        UUID id,
        Integer roundNumber,
        String roundName,
        RoundStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer maxParticipants,
        Integer advanceCount,
        Boolean isFinalRound,
        // Game config
        UUID gameTypeId,
        String gameTypeName,
        Integer coinPerSession,
        List<UUID> selectedPresetIds,
        Map<String, List<UUID>> presetSubCategoryConfig,
        // Quiz config
        List<RoundQuizBriefResponse> quizzes
) {
}
