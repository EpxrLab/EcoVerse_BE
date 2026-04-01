package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.CampaignType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record CampaignDetailResponse(
        UUID id,
        String campaignCode,
        String campaignName,
        CampaignType campaignType,
        String description,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime invitationDate,
        LocalDateTime invitationDeadline,
        Integer topRankingCount,
        Integer totalRounds,
        List<CampaignRoundInfoResponse> rounds,
        List<CampaignParticipantInfoResponse> participants
) {
}

