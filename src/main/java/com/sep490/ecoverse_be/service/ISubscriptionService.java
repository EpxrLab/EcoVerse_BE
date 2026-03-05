package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionRequest;
import com.sep490.ecoverse_be.dto.request.RenewSubscriptionRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionResponse;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import org.springframework.data.domain.Pageable;

public interface ISubscriptionService {

    // School/Partnership subscribes to a plan (creates subscription + payment via PayOS)
    PaymentResponse subscribe(CreateSubscriptionRequest request, Long userId);

    // Renew an existing or expired subscription
    PaymentResponse renewSubscription(RenewSubscriptionRequest request, Long userId);

    // Get current active subscription for the logged-in user
    SubscriptionResponse getMySubscription(Long userId);

    // Get subscription history for the logged-in user
    PageResponse<SubscriptionResponse> getMySubscriptionHistory(Long userId, Pageable pageable);

    // Get subscription by ID
    SubscriptionResponse getSubscriptionById(Long subscriptionId);

    // Admin: list all subscriptions with filters
    PageResponse<SubscriptionResponse> getAllSubscriptions(SubscriberType subscriberType,
                                                           SubscriptionStatus status,
                                                           String keyword,
                                                           Pageable pageable);

    // Cancel a subscription
    SubscriptionResponse cancelSubscription(Long subscriptionId, String reason, Long userId);
}
