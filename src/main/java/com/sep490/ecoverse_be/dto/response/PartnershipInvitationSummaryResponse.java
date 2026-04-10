package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PartnershipInvitationSummaryResponse(
        UUID invitationId,
        UUID campaignId,
        String campaignCode,
        String campaignName,
        ParticipationStatus status,
        LocalDateTime invitationSentAt,
        LocalDateTime participationConfirmedAt,
        Integer studentsEnrolled,
        String partnershipName
) {
}
