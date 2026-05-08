package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ParentCampaignInvitationHistoryResponse(
        UUID campaignId,
        List<ParentInvitationRoundResponse> rounds,
        String campaignName,
        String campaignStatus,
        UUID studentId,
        String studentName,
        ParticipationStatus parentApprovalStatus,
        String rejectionReason,
        OffsetDateTime invitationDeadline,
        OffsetDateTime campaignEndDate
) {
}
