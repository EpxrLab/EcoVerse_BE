package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        String subscriptionCode,
        SubscriberType subscriberType,
        String subscriberName,
        Long planId,
        String planCode,
        String planName,
        SubscriptionStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean autoRenew,
        String cancellationReason,
        LocalDateTime cancelledAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
