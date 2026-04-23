package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public record PartnershipInvitationSummaryResponse(
        UUID invitationId,
        UUID campaignId,
        String campaignCode,
        String campaignName,
        ParticipationStatus status,
        PartnershipCampaignStatus campaignPartnershipStatus,
        OffsetDateTime invitationSentAt,
        OffsetDateTime participationConfirmedAt,
        Integer studentsEnrolled,
        String partnershipName,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime registrationDeadline,
        Integer maxStudentsPerSchool
) {
}
