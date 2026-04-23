package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
        Integer maxAiQuizGenerationsPerPeriod,
        Map<String, Object> features,
        int gracePeriodDays,
        boolean isActive,
        int displayOrder,
        String createdByEmail,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
