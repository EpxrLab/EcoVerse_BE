package com.sep490.ecoverse_be.mapper;

import com.sep490.ecoverse_be.dto.response.SubscriptionTransactionResponse;
import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.dto.response.SubscriptionResponse;
import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.enums.SubscriberType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubscriptionMapper {

    public SubscriptionResponse toResponse(Subscription subscription,
                                           List<SubscriptionTransactionResponse> transactions,
                                           Long usedStudents,
                                           Long usedCampaignsCurrentMonth,
                                           Long usedAiQuizGenerations) {
        String subscriberName = null;
        if (subscription.getSubscriberType() == SubscriberType.SCHOOL && subscription.getSchool() != null) {
            subscriberName = subscription.getSchool().getSchoolName();
        } else if (subscription.getSubscriberType() == SubscriberType.PARTNERSHIP && subscription.getPartnership() != null) {
            subscriberName = subscription.getPartnership().getOrganizationName();
        }

        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getSubscriptionCode(),
                subscription.getSubscriberType(),
                subscriberName,
                subscription.getPlan().getId(),
                subscription.getPlan().getPlanCode(),
                subscription.getPlan().getPlanName(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.isAutoRenew(),
                subscription.getCancellationReason(),
                subscription.getCancelledAt(),
                subscription.getNotes(),
                subscription.getPlan().getMaxStudents(),
                usedStudents,
                subscription.getPlan().getMaxCampaignsPerMonth(),
                usedCampaignsCurrentMonth,
                subscription.getPlan().getMaxRoundsPerCampaign(),
                subscription.getPlan().getMaxSchoolsPerCampaign(),
                subscription.getPlan().getMaxAiQuizGenerations(),
                usedAiQuizGenerations,
                transactions,
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    public SubscriptionTransactionResponse toTransactionResponse(Payment payment) {
        return new SubscriptionTransactionResponse(
                payment.getId(),
                payment.getPaymentCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getTransactionRef(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
