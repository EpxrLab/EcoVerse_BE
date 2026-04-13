package com.sep490.ecoverse_be.mapper;

import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.enums.SubscriberType;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, null);
    }

    public PaymentResponse toResponse(Payment payment, String checkoutUrl) {
        String subscriberName = null;
        if (payment.getSubscriberType() == SubscriberType.SCHOOL && payment.getSchool() != null) {
            subscriberName = payment.getSchool().getSchoolName();
        } else if (payment.getSubscriberType() == SubscriberType.PARTNERSHIP && payment.getPartnership() != null) {
            subscriberName = payment.getPartnership().getOrganizationName();
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentCode(),
                payment.getSubscriberType(),
                subscriberName,
                payment.getSubscription().getId(),
                payment.getSubscription().getSubscriptionCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionRef(),
                payment.getPaidAt(),
                payment.getFailureReason(),
                payment.getPayerName(),
                payment.getPayerEmail(),
                payment.getPayerPhone(),
                checkoutUrl,
                payment.getCreatedAt()
        );
    }
}
