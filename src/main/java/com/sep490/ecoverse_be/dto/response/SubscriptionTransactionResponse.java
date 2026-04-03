package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionTransactionResponse(
        UUID paymentId,
        String paymentCode,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String transactionRef,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}

