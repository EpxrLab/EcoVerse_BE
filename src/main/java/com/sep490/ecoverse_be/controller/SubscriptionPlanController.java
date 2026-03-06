package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.SubscriptionPlanResponse;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ISubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/subscription-plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
public class SubscriptionPlanController {

    private final ISubscriptionPlanService subscriptionPlanService;

    @PostMapping
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        SubscriptionPlanResponse response = subscriptionPlanService.createPlan(
                request, principal.getUser().getId()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseDto.created(response, "Subscription plan created successfully."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request
    ) {
        SubscriptionPlanResponse response = subscriptionPlanService.updatePlan(id, request);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plan updated successfully."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> getPlanById(@PathVariable UUID id) {
        SubscriptionPlanResponse response = subscriptionPlanService.getPlanById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plan retrieved successfully."));
    }

    @GetMapping("/code/{planCode}")
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> getPlanByCode(@PathVariable String planCode) {
        SubscriptionPlanResponse response = subscriptionPlanService.getPlanByCode(planCode);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plan retrieved successfully."));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<PageResponse<SubscriptionPlanResponse>>> getAllPlans(
            @RequestParam(required = false) SubscriberType subscriberType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "displayOrder") Pageable pageable
    ) {
        PageResponse<SubscriptionPlanResponse> response = subscriptionPlanService.getAllPlans(
                subscriberType, isActive, keyword, pageable
        );
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plans retrieved successfully."));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ResponseDto<Void>> toggleActiveStatus(@PathVariable UUID id) {
        subscriptionPlanService.toggleActiveStatus(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Subscription plan active status toggled successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deletePlan(@PathVariable UUID id) {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Subscription plan deleted successfully."));
    }
}
