package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreateRewardRequest;
import com.sep490.ecoverse_be.dto.request.UpdateRewardRequest;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.RewardResponse;
import com.sep490.ecoverse_be.enums.RewardType;
import com.sep490.ecoverse_be.service.IRewardService;
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
@RequestMapping("/api/school/rewards")
@PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
@Tag(name = "School - Reward Management", description = "APIs quản lý quà thưởng dành cho Trường học")
public class RewardController {

    @Autowired
    private IRewardService rewardService;

    @PostMapping
    @Operation(
            summary = "Tạo quà thưởng mới",
            description = """
                    Tạo một phần thưởng mới trong hệ thống. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    - Nếu `isUnlimited = true` thì không cần truyền `stockQuantity`.
                    - Nếu `isUnlimited = false` thì bắt buộc truyền `stockQuantity >= 0`.
                    - Tên quà không được trùng với quà khác cùng trường đang hoạt động.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<RewardResponse>> createReward(
            @Valid @RequestBody CreateRewardRequest request) {
        RewardResponse response = rewardService.createReward(request);
        return ResponseEntity.status(201).body(ResponseDto.created(response, "Tạo quà thành công"));
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách quà của trường",
            description = """
                    Lấy toàn bộ danh sách quà đang hoạt động của trường đang đăng nhập.
                    Có thể lọc theo loại quà (`rewardType`).

                    **Query param (tùy chọn):** `rewardType` = PHYSICAL | DIGITAL | VOUCHER

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<List<RewardResponse>>> getMyRewards(
            @RequestParam(required = false) RewardType rewardType) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardService.getRewardsForSchool(rewardType), "Lấy danh sách quà thành công"));
    }

    @GetMapping("/student")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(
            summary = "Lấy danh sách quà của trường",
            description = """
                    Lấy toàn bộ danh sách quà đang hoạt động của trường dành cho học sinh.
                    Có thể lọc theo loại quà (`rewardType`).

                    **Query param (tùy chọn):** `rewardType` = PHYSICAL | DIGITAL | VOUCHER

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<List<RewardResponse>>> getRewardsForStudent(
            @RequestParam(required = false) RewardType rewardType) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardService.getRewardsForStudent(rewardType), "Lấy danh sách quà thành công"));
    }

    @GetMapping("/parent")
    @PreAuthorize("hasAuthority('PARENT')")
    @Operation(
            summary = "Lấy danh sách quà của trường",
            description = """
                    Lấy toàn bộ danh sách quà đang hoạt động của trường dành cho phụ huynh đang đăng nhập.
                    Có thể lọc theo loại quà (`rewardType`).

                    **Query param (tùy chọn):** `rewardType` = PHYSICAL | DIGITAL | VOUCHER

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<List<RewardResponse>>> getRewardsForParent(
            @RequestParam(required = false) RewardType rewardType) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardService.getRewardsForParent(rewardType), "Lấy danh sách quà thành công"));
    }

    @GetMapping("/{rewardId}")
    @Operation(
            summary = "Lấy chi tiết một quà",
            description = """
                    Lấy thông tin đầy đủ của một phần thưởng theo ID.
                    Chỉ trả về quà do chính trường sở hữu.

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy quà hoặc quà đã bị xóa
                    """
    )
    public ResponseEntity<ResponseDto<RewardResponse>> getRewardById(@PathVariable UUID rewardId) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardService.getRewardById(rewardId), "Lấy chi tiết quà thành công"));
    }

    @PutMapping("/{rewardId}")
    @Operation(
            summary = "Cập nhật thông tin quà (partial update)",
            description = """
                    Cập nhật thông tin của một phần thưởng. Hỗ trợ partial update.

                    - Nếu chuyển `isUnlimited = true` thì `stockQuantity` sẽ tự động xóa.
                    - Nếu chuyển `isUnlimited = false` cần truyền thêm `stockQuantity`.

                    **Lỗi có thể xảy ra:**
                    - `400` — Tên quà bị trùng
                    - `404` — Không tìm thấy quà
                    """
    )
    public ResponseEntity<ResponseDto<RewardResponse>> updateReward(
            @PathVariable UUID rewardId,
            @Valid @RequestBody UpdateRewardRequest request) {
        return ResponseEntity.ok(
                ResponseDto.success(rewardService.updateReward(rewardId, request), "Cập nhật quà thành công"));
    }

    @DeleteMapping("/{rewardId}")
    @Operation(
            summary = "Xóa mềm quà thưởng",
            description = """
                    Soft delete phần thưởng (đặt `isActive = false`).
                    Quà sẽ không hiển thị trong danh sách nhưng vẫn còn trong DB.

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy quà
                    """
    )
    public ResponseEntity<ResponseDto<Void>> deleteReward(@PathVariable UUID rewardId) {
        rewardService.deleteReward(rewardId);
        return ResponseEntity.ok(ResponseDto.success(null, "Xóa quà thành công"));
    }
}
