package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết lời mời + thông tin campaign (không gồm trạng thái lifecycle partnership của campaign).
 */
@Builder
public record PartnershipInvitationDetailResponse(
        UUID invitationId,

        UUID campaignId,
        String campaignCode,
        String campaignName,
        String description,
        String bannerImageUrl,

        ParticipationStatus status,
        OffsetDateTime invitationSentAt,
        OffsetDateTime participationConfirmedAt,

        OffsetDateTime registrationDate,
        OffsetDateTime registrationDeadline,
        OffsetDateTime invitationDate,
        OffsetDateTime invitationDeadline,
        OffsetDateTime startDate,
        OffsetDateTime endDate,

        Integer maxStudentsPerSchool,
        Integer minStudentsPerSchool,
        Integer totalStudentQuota,
        Integer totalRounds,
        Integer studentsEnrolled,

        String partnershipName,

        Integer topRankingCount,
        List<PartnershipInvitationRoundBriefResponse> rounds,
        List<CampaignRewardResponse> rewards
) {
}
