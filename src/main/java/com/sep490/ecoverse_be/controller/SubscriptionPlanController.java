package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.SubscriptionPlanResponse;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ISubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequiredArgsConstructor
@Tag(name = "Subscription Plan", description = "APIs quản lý gói đăng ký (public GET + admin CRUD)")
public class SubscriptionPlanController {

    private final ISubscriptionPlanService subscriptionPlanService;

    // ==================== Public APIs (không cần đăng nhập) ====================

    @GetMapping("/api/subscription-plans")
    @Operation(summary = "Lấy danh sách gói đăng ký (public)",
            description = "Ai cũng có thể xem danh sách gói đăng ký. Hỗ trợ lọc theo loại, trạng thái, tìm kiếm, phân trang.")
    public ResponseEntity<ResponseDto<PageResponse<SubscriptionPlanResponse>>> getAllPlans(
            @RequestParam(required = false) SubscriberType subscriberType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
        PageResponse<SubscriptionPlanResponse> response = subscriptionPlanService.getAllPlans(
                subscriberType, isActive, keyword, pageable
        );
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plans retrieved successfully."));
    }

    @GetMapping("/api/subscription-plans/{id}")
    @Operation(summary = "Xem chi tiết gói đăng ký (public)",
            description = "Ai cũng có thể xem chi tiết một gói đăng ký theo ID.")
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> getPlanById(@PathVariable UUID id) {
        SubscriptionPlanResponse response = subscriptionPlanService.getPlanById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plan retrieved successfully."));
    }

    @GetMapping("/api/subscription-plans/code/{planCode}")
    @Operation(summary = "Xem chi tiết gói đăng ký theo mã (public)",
            description = "Ai cũng có thể xem chi tiết một gói đăng ký theo planCode.")
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> getPlanByCode(@PathVariable String planCode) {
        SubscriptionPlanResponse response = subscriptionPlanService.getPlanByCode(planCode);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plan retrieved successfully."));
    }

    // ==================== Admin APIs (chỉ ADMINISTRATOR) ====================

    @PostMapping("/api/admin/subscription-plans")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @Operation(summary = "Tạo gói đăng ký mới (Admin)")
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

    @PutMapping("/api/admin/subscription-plans/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @Operation(summary = "Cập nhật gói đăng ký (Admin)")
    public ResponseEntity<ResponseDto<SubscriptionPlanResponse>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request
    ) {
        SubscriptionPlanResponse response = subscriptionPlanService.updatePlan(id, request);
        return ResponseEntity.ok(ResponseDto.success(response, "Subscription plan updated successfully."));
    }

    @PatchMapping("/api/admin/subscription-plans/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @Operation(summary = "Bật/tắt trạng thái gói đăng ký (Admin)")
    public ResponseEntity<ResponseDto<Void>> toggleActiveStatus(@PathVariable UUID id) {
        subscriptionPlanService.toggleActiveStatus(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Subscription plan active status toggled successfully."));
    }

    @DeleteMapping("/api/admin/subscription-plans/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @Operation(summary = "Xóa gói đăng ký (Admin)")
    public ResponseEntity<ResponseDto<Void>> deletePlan(@PathVariable UUID id) {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Subscription plan deleted successfully."));
    }
}
