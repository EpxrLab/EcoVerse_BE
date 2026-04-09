package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.CampaignType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CampaignSummaryResponse(
        UUID id,
        String campaignCode,
        String campaignName,
        CampaignType campaignType,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime registrationDate,
        LocalDateTime registrationDateDeadline,
        LocalDateTime invitationDate,
        LocalDateTime invitationDeadline,
        String description,
        boolean hasQuiz,
        boolean hasGame
) {
}
