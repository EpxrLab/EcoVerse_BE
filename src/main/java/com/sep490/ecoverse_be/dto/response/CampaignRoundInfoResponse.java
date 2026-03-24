package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RoundStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CampaignRoundInfoResponse(
        UUID id,
        Integer roundNumber,
        String roundName,
        RoundStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        UUID quizId
) {
}

