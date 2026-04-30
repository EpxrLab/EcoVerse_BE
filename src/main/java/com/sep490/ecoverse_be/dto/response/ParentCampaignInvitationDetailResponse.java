package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.CampaignType;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ParentCampaignInvitationDetailResponse(
        UUID campaignId,
        String campaignCode,
        String campaignName,
        CampaignType campaignType,
        String status,
        String description,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime invitationDeadline,
        String bannerImageUrl,
        String bannerImagePresignedUrl,
        List<ParentInvitationRoundResponse> rounds,
        List<CampaignParticipantInfoResponse> invitedChildren
) {
}
