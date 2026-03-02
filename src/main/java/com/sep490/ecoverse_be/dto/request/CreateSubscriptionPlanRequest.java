package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.SubscriberType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record CreateSubscriptionPlanRequest(

        @NotBlank(message = "Plan code is required")
        @Size(max = 50, message = "Plan code must not exceed 50 characters")
        String planCode,

        @NotBlank(message = "Plan name is required")
        @Size(max = 255, message = "Plan name must not exceed 255 characters")
        String planName,

        @NotNull(message = "Subscriber type is required")
        SubscriberType subscriberType,

        String description,

        @NotNull(message = "Duration days is required")
        @Min(value = 1, message = "Duration must be at least 1 day")
        Integer durationDays,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must be >= 0")
        BigDecimal price,

        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        @Min(value = 1, message = "Max students must be at least 1")
        Integer maxStudents,

        @Min(value = 1, message = "Max campaigns per month must be at least 1")
        Integer maxCampaignsPerMonth,

        @Min(value = 1, message = "Max rounds per campaign must be at least 1")
        Integer maxRoundsPerCampaign,

        @Min(value = 1, message = "Max schools per campaign must be at least 1")
        Integer maxSchoolsPerCampaign,

        Map<String, Object> features,

        @Min(value = 0, message = "Grace period days must be >= 0")
        Integer gracePeriodDays,

        @Min(value = 0, message = "Display order must be >= 0")
        Integer displayOrder
) {
}
