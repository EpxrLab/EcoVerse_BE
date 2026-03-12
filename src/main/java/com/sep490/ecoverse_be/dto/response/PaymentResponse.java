package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.PaymentMethod;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import com.sep490.ecoverse_be.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String paymentCode,
        SubscriberType subscriberType,
        String subscriberName,
        UUID subscriptionId,
        String subscriptionCode,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String transactionRef,
        LocalDateTime paidAt,
        String failureReason,
        String payerName,
        String payerEmail,
        String payerPhone,
        String checkoutUrl,
        LocalDateTime createdAt
) {
}
