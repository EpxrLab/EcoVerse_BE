package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CancelRewardRequestDto;
import com.sep490.ecoverse_be.dto.request.CreateRewardRequestDto;
import com.sep490.ecoverse_be.dto.request.MarkDeliveredRequest;
import com.sep490.ecoverse_be.dto.request.RejectRewardRequestDto;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.RewardRequestResponse;
import com.sep490.ecoverse_be.dto.response.RewardRequestTrackingResponse;
import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import com.sep490.ecoverse_be.service.IRewardRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Reward Request", description = "APIs đổi quà thưởng: học sinh/phụ huynh tạo yêu cầu, trường quản lý và giao quà")
public class RewardRequestController {

    @Autowired
    private IRewardRequestService rewardRequestService;

    @PostMapping("/api/rewards/requests")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @Operation(
            summary = "Tạo yêu cầu đổi quà",
            description = """
                    Học sinh hoặc phụ huynh tạo yêu cầu đổi quà.

                    **Quy tắc:**
                    - Coin sẽ bị trừ ngay khi tạo (status = PENDING) để tránh spam.
                    - Hệ thống kiểm tra và giữ chỗ (reserve) tồn kho trước khi tạo. Nếu hết hàng thì báo lỗi.
                    - Phụ huynh phải truyền thêm `studentId` (là con của mình) trong request body.

                    **Lỗi có thể xảy ra:**
                    - `400` — Không đủ coin, hết hàng, studentId là bắt buộc với phụ huynh
                    - `404` — Không tìm thấy quà hoặc học sinh
                    """
    )
    public ResponseEntity<ResponseDto<RewardRequestResponse>> createRequest(
            @Valid @RequestBody CreateRewardRequestDto request) {
        RewardRequestResponse response = rewardRequestService.createRequest(request);
        return ResponseEntity.status(201).body(ResponseDto.created(response, "Tạo yêu cầu đổi quà thành công"));
    }

    @GetMapping("/api/rewards/requests/my")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @Operation(
            summary = "Xem lịch sử yêu cầu đổi quà của mình",
            description = """
                    - **Student**: trả về danh sách request của chính học sinh đó.
                    - **Parent**: trả về danh sách request của tất cả con.
                    """
    )
    public ResponseEntity<ResponseDto<List<RewardRequestResponse>>> getMyRequests() {
        return ResponseEntity.ok(
                ResponseDto.success(rewardRequestService.getMyRequests(), "Lấy danh sách yêu cầu thành công"));
    }

    @GetMapping("/api/rewards/requests/{requestId}/tracking")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT', 'PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Theo dõi timeline trạng thái yêu cầu đổi quà",
            description = """
                    Trả về timeline cơ bản các mốc trạng thái của request:
                    PENDING (tạo) -> APPROVED/REJECTED -> DELIVERED -> CONFIRMED hoặc CANCELLED.

                    Bao gồm thông tin actor theo mốc (nếu có), đặc biệt có `cancelledBy`.
                    """
    )
    public ResponseEntity<ResponseDto<RewardRequestTrackingResponse>> getRequestTracking(@PathVariable UUID requestId) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardRequestService.getRequestTracking(requestId), "Lấy timeline tracking thành công"));
    }

    @PutMapping("/api/rewards/requests/{requestId}/cancel")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @Operation(
            summary = "Hủy yêu cầu đổi quà",
            description = """
                    Chỉ có thể hủy khi trạng thái đang là **PENDING**.
                    Coin và số lượng kho sẽ được hoàn trả ngay sau khi hủy.

                    **Lỗi có thể xảy ra:**
                    - `400` — Trạng thái không phải PENDING
                    - `404` — Không tìm thấy yêu cầu
                    """
    )
    public ResponseEntity<ResponseDto<RewardRequestResponse>> cancelRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) CancelRewardRequestDto dto) {
        if (dto == null) dto = new CancelRewardRequestDto();
        return ResponseEntity.ok(
                ResponseDto.success(rewardRequestService.cancelRequest(requestId, dto), "Hủy yêu cầu thành công"));
    }

    @PutMapping("/api/rewards/requests/{requestId}/confirm")
    @PreAuthorize("hasAuthority('PARENT')")
    @Operation(
            summary = "Phụ huynh xác nhận đã nhận quà",
            description = """
                    Phụ huynh xác nhận đã nhận quà sau khi trường giao (DELIVERED -> CONFIRMED).
                    Chỉ phụ huynh có liên kết với học sinh trong yêu cầu mới được xác nhận.

                    **Lỗi có thể xảy ra:**
                    - `400` — Trạng thái chưa là DELIVERED
                    - `404` — Không tìm thấy yêu cầu
                    """
    )
    public ResponseEntity<ResponseDto<RewardRequestResponse>> confirmReceived(@PathVariable UUID requestId) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardRequestService.confirmReceived(requestId), "Xác nhận nhận quà thành công"));
    }


    @GetMapping("/api/school/rewards/requests")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Trường xem danh sách yêu cầu đổi quà",
            description = """
                    Lấy toàn bộ yêu cầu đổi quà của trường, có thể lọc theo `status`.

                    **Query param (tùy chọn):** `status` = PENDING | APPROVED | DELIVERED | CONFIRMED | REJECTED | CANCELLED
                    """
    )
    public ResponseEntity<ResponseDto<List<RewardRequestResponse>>> getSchoolRequests(
            @RequestParam(required = false) RewardRequestStatus status) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardRequestService.getSchoolRequests(status), "Lấy danh sách yêu cầu thành công"));
    }

    @PutMapping("/api/school/rewards/requests/{requestId}/approve")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Trường duyệt hoặc từ chối yêu cầu đổi quà (PENDING -> APPROVED / REJECTED)",
            description = """
                    Chỉ có thể duyệt hoặc từ chối khi trạng thái đang là **PENDING**.
                    Coin và số lượng kho sẽ được hoàn trả ngay nếu bị từ chối.

                    **Lỗi có thể xảy ra:**
                    - `400` — Trạng thái không phải PENDING
                    - `404` — Không tìm thấy yêu cầu
                    """
    )
    public ResponseEntity<ResponseDto<RewardRequestResponse>> approveRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectRewardRequestDto dto) {
        return ResponseEntity.ok(
                ResponseDto.success(
                        rewardRequestService.approveOrRejectRequest(requestId, dto),
                        "Duyệt hoặc từ chối yêu cầu thành công"));
    }

    @PutMapping("/api/school/rewards/requests/{requestId}/deliver")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Trường xác nhận đã giao quà (APPROVED -> DELIVERED)",
            description = """
                    Chỉ có thể xác nhận giao khi trạng thái đang là **APPROVED**.
                    Sau khi DELIVERED, coin không thể hoàn trả nữa.

                    `imageUrl` (tùy chọn): S3 key của ảnh bằng chứng giao quà, lấy từ `POST /api/files/upload/image`.

                    **Lỗi có thể xảy ra:**
                    - `400` — Trạng thái không phải APPROVED
                    - `404` — Không tìm thấy yêu cầu
                    """
    )
    public ResponseEntity<ResponseDto<RewardRequestResponse>> markDelivered(
            @PathVariable UUID requestId,
            @RequestBody(required = false) MarkDeliveredRequest body) {
        String imageUrl = body != null ? body.getImageUrl() : null;
        return ResponseEntity.ok(
                ResponseDto.success(rewardRequestService.markDelivered(requestId, imageUrl), "Xác nhận giao quà thành công"));
    }
}
