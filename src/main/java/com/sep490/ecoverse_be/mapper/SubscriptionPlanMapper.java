package com.sep490.ecoverse_be.mapper;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.response.SubscriptionPlanResponse;
import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionPlanMapper {

    private final ModelMapper modelMapper;

    public SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getPlanCode(),
                plan.getPlanName(),
                plan.getSubscriberType(),
                plan.getDescription(),
                plan.getDurationDays(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getMaxStudents(),
                plan.getMaxCampaignsPerMonth(),
                plan.getMaxRoundsPerCampaign(),
                plan.getMaxSchoolsPerCampaign(),
                plan.getFeatures(),
                plan.getGracePeriodDays(),
                plan.isActive(),
                plan.getDisplayOrder(),
                plan.getCreatedBy() != null ? plan.getCreatedBy().getEmail() : null,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    public SubscriptionPlan toEntity(CreateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = modelMapper.map(request, SubscriptionPlan.class);
        if (request.currency() == null) {
            plan.setCurrency("VND");
        }
        if (request.gracePeriodDays() == null) {
            plan.setGracePeriodDays(30);
        }
        if (request.displayOrder() == null) {
            plan.setDisplayOrder(0);
        }
        plan.setActive(true);
        return plan;
    }

    public void updateEntity(SubscriptionPlan plan, UpdateSubscriptionPlanRequest request) {
        if (request.planName() != null) {
            plan.setPlanName(request.planName());
        }
        if (request.description() != null) {
            plan.setDescription(request.description());
        }
        if (request.durationDays() != null) {
            plan.setDurationDays(request.durationDays());
        }
        if (request.price() != null) {
            plan.setPrice(request.price());
        }
        if (request.currency() != null) {
            plan.setCurrency(request.currency());
        }
        if (request.maxStudents() != null) {
            plan.setMaxStudents(request.maxStudents());
        }
        if (request.maxCampaignsPerMonth() != null) {
            plan.setMaxCampaignsPerMonth(request.maxCampaignsPerMonth());
        }
        if (request.maxRoundsPerCampaign() != null) {
            plan.setMaxRoundsPerCampaign(request.maxRoundsPerCampaign());
        }
        if (request.maxSchoolsPerCampaign() != null) {
            plan.setMaxSchoolsPerCampaign(request.maxSchoolsPerCampaign());
        }
        if (request.features() != null) {
            plan.setFeatures(request.features());
        }
        if (request.gracePeriodDays() != null) {
            plan.setGracePeriodDays(request.gracePeriodDays());
        }
        if (request.displayOrder() != null) {
            plan.setDisplayOrder(request.displayOrder());
        }
    }
}
