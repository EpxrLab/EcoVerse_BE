package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CampaignRewardResponse(
        UUID id,
        UUID campaignId,
        Integer rankPosition,
        String rewardName,
        String description,
        String imageUrl,
        String imagePresignedUrl,
        String sponsorName,
        PartnershipRewardStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
