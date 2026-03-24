package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.Map;

@Builder
public record AdminCampaignAnalyticsResponse(
        long totalCampaigns,
        long totalSchoolCampaigns,
        long totalPartnershipCampaigns,
        long totalParticipants,
        long totalSchoolInvitations,
        long approvedSchoolInvitations,
        Map<String, Long> schoolCampaignStatusCounts,
        Map<String, Long> partnershipCampaignStatusCounts
) {
}

