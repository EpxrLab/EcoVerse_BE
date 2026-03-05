package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionPlanResponse;
import com.sep490.ecoverse_be.enums.SubscriberType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ISubscriptionPlanService {

    SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request, UUID adminUserId);

    SubscriptionPlanResponse updatePlan(UUID planId, UpdateSubscriptionPlanRequest request);

    SubscriptionPlanResponse getPlanById(UUID planId);

    SubscriptionPlanResponse getPlanByCode(String planCode);

    PageResponse<SubscriptionPlanResponse> getAllPlans(
            SubscriberType subscriberType,
            Boolean isActive,
            String keyword,
            Pageable pageable
    );

    void toggleActiveStatus(UUID planId);

    void deletePlan(UUID planId);
}
