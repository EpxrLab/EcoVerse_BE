package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.CampaignType;
import lombok.Builder;

import java.time.OffsetDateTime;
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
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime registrationDate,
        OffsetDateTime registrationDeadline,
        OffsetDateTime invitationDate,
        OffsetDateTime invitationDeadline,
        Integer maxStudentsPerSchool,
        Integer minStudentsPerSchool,
        Integer totalStudentQuota,
        Integer topRankingCount,
        Integer totalRounds,
        String bannerImageUrl,
        String bannerImagePresignedUrl,
        List<CampaignRoundInfoResponse> rounds,
        List<CampaignParticipantInfoResponse> participants,
        List<InvitedSchoolInfoResponse> invitedSchools
) {}
