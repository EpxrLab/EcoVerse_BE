package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.service.IAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
@Tag(name = "Admin", description = "Quản lý phê duyệt tài khoản trường học, đối tác và quản trị hệ thống")
public class AdminController {

    @Autowired
    private IAdminService adminService;

    @GetMapping("/schools/pending")
    @Operation(
            summary = "Lấy danh sách trường học đang chờ duyệt",
            description = """
                    Trả về danh sách tất cả trường học có `approvalStatus = PENDING`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Danh sách trường học chờ duyệt",
                      "data": [
                        {
                          "id": "uuid",
                          "userId": "uuid",
                          "schoolName": "Trường Tiểu Học Lê Văn Tám",
                          "schoolType": "PUBLIC",
                          "taxCode": "0312345678",
                          "contactEmail": "school@example.com",
                          "phoneNumber": "0901234567",
                          "address": "123 Lê Lợi",
                          "ward": "Quận 1",
                          "province": "Hồ Chí Minh",
                          "principalName": "Nguyễn Văn A",
                          "position": "Hiệu trưởng",
                          "logoUrl": "https://...",
                          "licenseUrl": "https://...",
                          "approvalStatus": "PENDING",
                          "createdAt": "2025-01-01T10:00:00"
                        }
                      ]
                    }
                    ```
                    """
    )
    public ResponseDto<List<SchoolDetailResponse>> getPendingSchools() {
        return ResponseDto.success(adminService.getPendingSchools(), "Danh sách trường học chờ duyệt");
    }

    @GetMapping("/partnerships/pending")
    @Operation(
            summary = "Lấy danh sách đối tác đang chờ duyệt",
            description = """
                    Trả về danh sách tất cả đối tác có `approvalStatus = PENDING`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseDto<List<PartnershipDetailResponse>> getPendingPartnerships() {
        return ResponseDto.success(adminService.getPendingPartnerships(), "Danh sách đối tác chờ duyệt");
    }

    @GetMapping("/schools/approved")
    @Operation(
            summary = "Lấy danh sách trường học đã được duyệt",
            description = """
                    Trả về danh sách tất cả trường học có `approvalStatus = APPROVED`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseDto<List<SchoolDetailResponse>> getApprovedSchools() {
        return ResponseDto.success(adminService.getApprovedSchools(), "Danh sách trường học đã duyệt");
    }

    @GetMapping("/partnerships/approved")
    @Operation(
            summary = "Lấy danh sách đối tác đã được duyệt",
            description = """
                    Trả về danh sách tất cả đối tác có `approvalStatus = APPROVED`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseDto<List<PartnershipDetailResponse>> getApprovedPartnerships() {
        return ResponseDto.success(adminService.getApprovedPartnerships(), "Danh sách đối tác đã duyệt");
    }

    @PutMapping("/schools/{id}/approve")
    @Operation(
            summary = "Duyệt hoặc từ chối tài khoản trường học",
            description = """
                    Phê duyệt hoặc từ chối tài khoản trường học đang ở trạng thái `PENDING`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    - Khi **APPROVED**: tài khoản trường được kích hoạt (`status = ACTIVE`), trường nhận email thông báo duyệt thành công.
                    - Khi **REJECTED**: trường nhận email thông báo từ chối kèm lý do. Bắt buộc phải cung cấp `reason`.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body:**
                    ```json
                    {
                      "status": "APPROVED",
                      "reason": null
                    }
                    ```
                    hoặc
                    ```json
                    {
                      "status": "REJECTED",
                      "reason": "Giấy phép hoạt động không hợp lệ"
                    }
                    ```

                    **status:** `APPROVED` | `REJECTED`

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy trường học
                    - `400` — Trường không ở trạng thái PENDING hoặc thiếu lý do từ chối
                    """
    )
    public ResponseDto<SchoolDetailResponse> approveSchool(@PathVariable UUID id, @RequestBody UpdateApprovalRequest request) {
        try {
            return ResponseDto.success(adminService.updateSchoolApproval(id, request), "Duyệt trường học thành công");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseDto.badRequest(null, e.getMessage());
        }
    }

    @PutMapping("/partnerships/{id}/approve")
    @Operation(
            summary = "Duyệt hoặc từ chối tài khoản đối tác",
            description = """
                    Phê duyệt hoặc từ chối tài khoản đối tác đang ở trạng thái `PENDING`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    - Khi **APPROVED**: tài khoản đối tác được kích hoạt (`status = ACTIVE`), đối tác nhận email thông báo.
                    - Khi **REJECTED**: đối tác nhận email thông báo từ chối kèm lý do. Bắt buộc phải cung cấp `reason`.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body:**
                    ```json
                    {
                      "status": "APPROVED",
                      "reason": null
                    }
                    ```

                    **status:** `APPROVED` | `REJECTED`

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy đối tác
                    - `400` — Đối tác không ở trạng thái PENDING hoặc thiếu lý do từ chối
                    """
    )
    public ResponseDto<PartnershipDetailResponse> approvePartnership(@PathVariable UUID id, @RequestBody UpdateApprovalRequest request) {
        try {
            return ResponseDto.success(adminService.updatePartnershipApproval(id, request), "Duyệt đối tác thành công");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseDto.badRequest(null, e.getMessage());
        }
    }

    @GetMapping("/users")
    @Operation(
            summary = "Lấy danh sách tất cả người dùng",
            description = """
                    Trả về danh sách người dùng toàn hệ thống, có thể lọc theo `role`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Quy tắc lọc:**
                    - Không truyền `role` → trả về tất cả loại user (school, partnership, student, parent).
                    - `role=PARTNERSHIP_SCHOOL` → chỉ trường học.
                    - `role=THIRD_PARTY_PARTNERSHIP` → chỉ đối tác.
                    - `role=STUDENT` → chỉ học sinh. Có thể lọc thêm `schoolId` để lấy học sinh của một trường cụ thể.
                    - `role=PARENT` → chỉ phụ huynh. Có thể lọc thêm `schoolId` để lấy phụ huynh có con em tại trường đó.

                    **Query params:**
                    - `role` *(tuỳ chọn)*: `PARTNERSHIP_SCHOOL` | `THIRD_PARTY_PARTNERSHIP` | `STUDENT` | `PARENT`
                    - `schoolId` *(tuỳ chọn, chỉ hiệu lực khi role là `STUDENT` hoặc `PARENT`)*: UUID của trường học

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseDto<List<AdminUserListResponse>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UUID schoolId) {
        return ResponseDto.success(adminService.getAllUsers(role, schoolId), "Danh sách người dùng");
    }

    @GetMapping("/users/{userId}")
    @Operation(
            summary = "Lấy chi tiết một người dùng",
            description = """
                    Trả về thông tin chi tiết của một người dùng theo `userId`.
                    Chỉ dành cho role **ADMINISTRATOR**.

                    Tự động phân tích role của user và trả về các field phù hợp:
                    - **PARTNERSHIP_SCHOOL** → bổ sung `displayName` là tên trường.
                    - **THIRD_PARTY_PARTNERSHIP** → bổ sung `displayName` là tên tổ chức.
                    - **STUDENT** → bổ sung `displayName`, `studentCode`, `className`, `gradeLevel`, `schoolName`.
                    - **PARENT** → bổ sung `displayName`, `phoneNumber`, `schoolName` (trường của con đầu tiên).

                    **Path variable:** `userId` — UUID của bản ghi User

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy người dùng
                    """
    )
    public ResponseDto<AdminUserListResponse> getUserDetail(@PathVariable UUID userId) {
        try {
            return ResponseDto.success(adminService.getUserDetail(userId), "Chi tiết người dùng");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        }
    }

    @GetMapping("/schools/{schoolId}")
    @Operation(
            summary = "Lấy chi tiết một trường học",
            description = """
                    Trả về thông tin đầy đủ của một trường học theo `schoolId` (là UUID của bản ghi School, không phải userId).
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Path variable:** `schoolId` — UUID của bản ghi School

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy trường học
                    """
    )
    public ResponseDto<SchoolDetailResponse> getSchoolById(@PathVariable UUID schoolId) {
        try {
            return ResponseDto.success(adminService.getSchoolById(schoolId), "Chi tiết trường học");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        }
    }

    @GetMapping("/partnerships/{partnershipId}")
    @Operation(
            summary = "Lấy chi tiết một đối tác",
            description = """
                    Trả về thông tin đầy đủ của một đối tác theo `partnershipId` (là UUID của bản ghi Partnership, không phải userId).
                    Chỉ dành cho role **ADMINISTRATOR**.

                    **Path variable:** `partnershipId` — UUID của bản ghi Partnership

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy đối tác
                    """
    )
    public ResponseDto<PartnershipDetailResponse> getPartnershipById(@PathVariable UUID partnershipId) {
        try {
            return ResponseDto.success(adminService.getPartnershipById(partnershipId), "Chi tiết đối tác");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        }
    }

    @PutMapping("/de-active/user/{userId}")
    @Operation(
            summary = "Kích hoạt hoặc khóa tài khoản người dùng",
            description = """
                    Bật/tắt trạng thái hoạt động của một tài khoản bất kỳ. Chỉ dành cho role **ADMINISTRATOR**.

                    - `true` → Kích hoạt: `status = ACTIVE`, `isActive = true`
                    - `false` → Khóa: `status = INACTIVE`, `isActive = false`

                    Dùng để block trường học, đối tác, phụ huynh hoặc học sinh khi vi phạm.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body:** `true` hoặc `false` (boolean thuần)

                    **Path variable:** `userId` — UUID của bản ghi User

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Thành công",
                      "data": null
                    }
                    ```
                    """
    )
    public ResponseDto<String> blockUser(@PathVariable UUID userId, @RequestBody boolean isActive) {
        adminService.updateUserStatus(userId, isActive);
        return ResponseDto.success(null, "Thành công");
    }
}
