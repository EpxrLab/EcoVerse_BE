package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public record CampaignSummaryResponse(
        UUID id,
        String campaignCode,
        String campaignName,
        CampaignType campaignType,
        String status,
        ParticipationStatus participationStatus,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime registrationDate,
        OffsetDateTime registrationDateDeadline,
        OffsetDateTime invitationDate,
        OffsetDateTime invitationDeadline,
        String description,
        boolean hasQuiz,
        boolean hasGame
) {
}
