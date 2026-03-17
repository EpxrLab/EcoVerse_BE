package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionRequest;
import com.sep490.ecoverse_be.dto.request.RenewSubscriptionRequest;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ISubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;

    /**
     * School/Partnership subscribes to a plan.
     * Creates subscription and returns PayOS checkout URL for paid plans.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    public ResponseEntity<ResponseDto<PaymentResponse>> subscribe(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentResponse response = subscriptionService.subscribe(request, principal.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.created(response, "Subscription created successfully."));
    }

    /**
     * Renew an expired subscription.
     */
    @PostMapping("/renew")
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    public ResponseEntity<ResponseDto<PaymentResponse>> renewSubscription(
            @Valid @RequestBody RenewSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentResponse response = subscriptionService.renewSubscription(request, principal.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.created(response, "Subscription renewal initiated."));
    }

    /**
     * Get current active subscription of the logged-in School/Partnership.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    public ResponseEntity<ResponseDto<SubscriptionResponse>> getMySubscription(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        SubscriptionResponse response = subscriptionService.getMySubscription(principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Current subscription retrieved."));
    }

    /**
     * Get subscription history of the logged-in School/Partnership.
     */
    @GetMapping("/my/history")
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    public ResponseEntity<ResponseDto<PageResponse<SubscriptionResponse>>> getMySubscriptionHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<SubscriptionResponse> response = subscriptionService.getMySubscriptionHistory(
                principal.getUser().getId(), status, keyword, pageable);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription history retrieved."));
    }

    /**
     * Cancel active subscription.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    public ResponseEntity<ResponseDto<SubscriptionResponse>> cancelSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(
                id, reason, principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription cancelled."));
    }

    // ==================== Admin APIs ====================

    /**
     * Admin: get subscription by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<SubscriptionResponse>> getSubscriptionById(@PathVariable UUID id) {
        SubscriptionResponse response = subscriptionService.getSubscriptionById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription retrieved."));
    }

    /**
     * Admin: list all subscriptions with filters.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<PageResponse<SubscriptionResponse>>> getAllSubscriptions(
            @RequestParam(required = false) SubscriberType subscriberType,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<SubscriptionResponse> response = subscriptionService.getAllSubscriptions(
                subscriberType, status, keyword, pageable);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscriptions retrieved."));
    }
}
