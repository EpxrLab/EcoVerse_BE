package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
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
        LocalDateTime invitationSentAt,
        LocalDateTime participationConfirmedAt,

        LocalDateTime registrationDate,
        LocalDateTime registrationDeadline,
        LocalDateTime invitationDate,
        LocalDateTime invitationDeadline,
        LocalDateTime startDate,
        LocalDateTime endDate,

        Integer maxStudentsPerSchool,
        Integer totalStudentQuota,
        Integer totalRounds,
        Integer studentsEnrolled,

        String partnershipName,

        Integer topRankingCount,
        List<PartnershipInvitationRoundBriefResponse> rounds,
        List<CampaignRewardResponse> rewards
) {
}
