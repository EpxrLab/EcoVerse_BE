package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionRequest;
import com.sep490.ecoverse_be.dto.request.RenewSubscriptionRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionResponse;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ISubscriptionService {

    PaymentResponse subscribe(CreateSubscriptionRequest request, UUID userId);

    PaymentResponse renewSubscription(RenewSubscriptionRequest request, UUID userId);

    SubscriptionResponse getMySubscription(UUID userId);

    PageResponse<SubscriptionResponse> getMySubscriptionHistory(UUID userId,
                                                                 SubscriptionStatus status,
                                                                 String keyword,
                                                                 Pageable pageable);

    SubscriptionResponse getSubscriptionById(UUID subscriptionId);

    PageResponse<SubscriptionResponse> getAllSubscriptions(SubscriberType subscriberType,
                                                           SubscriptionStatus status,
                                                           String keyword,
                                                           Pageable pageable);

    SubscriptionResponse activatePendingSubscription(UUID subscriptionId, UUID userId);

    SubscriptionResponse cancelSubscription(UUID subscriptionId, String reason, UUID userId);
}
