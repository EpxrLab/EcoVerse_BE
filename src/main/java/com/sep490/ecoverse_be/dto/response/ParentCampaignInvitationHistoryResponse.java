package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ParentCampaignInvitationHistoryResponse(
        UUID campaignId,
        String campaignName,
        String campaignStatus,
        UUID studentId,
        String studentName,
        ParticipationStatus parentApprovalStatus,
        String rejectionReason,
        LocalDateTime invitationDeadline,
        LocalDateTime campaignEndDate
) {
}
