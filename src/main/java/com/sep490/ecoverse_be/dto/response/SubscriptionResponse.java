package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String subscriptionCode,
        SubscriberType subscriberType,
        String subscriberName,
        UUID planId,
        String planCode,
        String planName,
        SubscriptionStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean autoRenew,
        String cancellationReason,
        LocalDateTime cancelledAt,
        String notes,
        List<SubscriptionTransactionResponse> transactions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
