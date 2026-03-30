package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.NotificationResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.UnreadCountResponse;
import com.sep490.ecoverse_be.enums.NotificationStatus;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "APIs quản lý thông báo trong ứng dụng: danh sách, đánh dấu đã đọc, đếm chưa đọc")
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách thông báo",
            description = """
                    Trả về danh sách thông báo của người dùng đang đăng nhập, sắp xếp mới nhất lên đầu.
                    
                    **Query params:**
                    - `status` (tùy chọn): UNREAD | READ | ARCHIVED — nếu bỏ trống thì lấy tất cả
                    - `page` (mặc định 0): số trang (bắt đầu từ 0)
                    - `size` (mặc định 20): số phần tử mỗi trang
                    """
    )
    public ResponseEntity<ResponseDto<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = principal.getUser().getId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> result = notificationService.getNotifications(userId, status, pageable);

        return ResponseEntity.ok(ResponseDto.success(result, "Lấy danh sách thông báo thành công"));
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Đếm số thông báo chưa đọc",
            description = "Trả về tổng số thông báo có trạng thái UNREAD của người dùng hiện tại."
    )
    public ResponseEntity<ResponseDto<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(
                ResponseDto.success(new UnreadCountResponse(count), "Đếm thông báo chưa đọc thành công")
        );
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Đánh dấu một thông báo là đã đọc",
            description = """
                    Chuyển trạng thái của một thông báo từ UNREAD sang READ.
                    
                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy thông báo
                    - `403` — Thông báo không thuộc về người dùng này
                    """
    )
    public ResponseEntity<ResponseDto<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        UUID userId = principal.getUser().getId();
        NotificationResponse response = notificationService.markAsRead(id, userId);

        return ResponseEntity.ok(ResponseDto.success(response, "Đánh dấu đã đọc thành công"));
    }

    @PatchMapping("/read-all")
    @Operation(
            summary = "Đánh dấu tất cả thông báo là đã đọc",
            description = "Chuyển toàn bộ thông báo UNREAD của người dùng hiện tại sang trạng thái READ."
    )
    public ResponseEntity<ResponseDto<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(ResponseDto.success(null, "Đánh dấu tất cả đã đọc thành công"));
    }
}
