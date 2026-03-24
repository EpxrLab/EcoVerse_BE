package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CampaignProgressResponse(
        UUID campaignId,
        String campaignName,
        String campaignStatus,
        ParticipationStatus parentApprovalStatus,
        Integer totalRounds,
        Integer completedRounds
) {
}

