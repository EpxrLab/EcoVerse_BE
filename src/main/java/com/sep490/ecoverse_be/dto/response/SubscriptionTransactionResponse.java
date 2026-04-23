package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record SubscriptionTransactionResponse(
        UUID paymentId,
        String paymentCode,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String transactionRef,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
}

