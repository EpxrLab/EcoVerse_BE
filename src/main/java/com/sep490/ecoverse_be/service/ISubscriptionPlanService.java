package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionPlanResponse;
import com.sep490.ecoverse_be.enums.SubscriberType;
import org.springframework.data.domain.Pageable;

public interface ISubscriptionPlanService {

    SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request, Long adminUserId);

    SubscriptionPlanResponse updatePlan(Long planId, UpdateSubscriptionPlanRequest request);

    SubscriptionPlanResponse getPlanById(Long planId);

    SubscriptionPlanResponse getPlanByCode(String planCode);

    PageResponse<SubscriptionPlanResponse> getAllPlans(
            SubscriberType subscriberType,
            Boolean isActive,
            String keyword,
            Pageable pageable
    );

    void toggleActiveStatus(Long planId);

    void deletePlan(Long planId);
}
