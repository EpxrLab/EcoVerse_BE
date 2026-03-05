package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record SubscriptionPlanResponse(
        UUID id,
        String planCode,
        String planName,
        SubscriberType subscriberType,
        String description,
        int durationDays,
        BigDecimal price,
        String currency,
        Integer maxStudents,
        Integer maxCampaignsPerMonth,
        Integer maxRoundsPerCampaign,
        Integer maxSchoolsPerCampaign,
        Map<String, Object> features,
        int gracePeriodDays,
        boolean isActive,
        int displayOrder,
        String createdByEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
