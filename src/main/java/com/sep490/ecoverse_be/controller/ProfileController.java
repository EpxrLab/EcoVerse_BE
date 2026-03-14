package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.UpdatePartnershipProfileRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSchoolProfileRequest;
import com.sep490.ecoverse_be.dto.response.ParentProfileResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipProfileResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.SchoolProfileResponse;
import com.sep490.ecoverse_be.dto.response.StudentProfileResponse;
import com.sep490.ecoverse_be.dto.response.UserMeResponse;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.IProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "APIs lấy thông tin cá nhân của người dùng đang đăng nhập")
public class ProfileController {

    @Autowired
    private IProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin cơ bản của user đang đăng nhập",
            description = """
                    Trả về thông tin cơ bản của tài khoản hiện tại (id, role, username/email).
                    Áp dụng cho **tất cả role**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<UserMeResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserMeResponse response = profileService.getMe(principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thành công"));
    }

    @GetMapping("/student")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(
            summary = "Lấy hồ sơ học sinh đang đăng nhập",
            description = """
                    Trả về đầy đủ thông tin hồ sơ học sinh. Chỉ dành cho role **STUDENT**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<StudentProfileResponse>> getStudentProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        StudentProfileResponse response = profileService.getStudentProfile(principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thành công"));
    }

    @GetMapping("/parent")
    @PreAuthorize("hasAuthority('PARENT')")
    @Operation(
            summary = "Lấy hồ sơ phụ huynh đang đăng nhập",
            description = """
                    Trả về thông tin phụ huynh kèm danh sách con em. Chỉ dành cho role **PARENT**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<ParentProfileResponse>> getParentProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        ParentProfileResponse response = profileService.getParentProfile(principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thành công"));
    }

    @GetMapping("/school")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Lấy hồ sơ trường học đang đăng nhập",
            description = """
                    Trả về thông tin chi tiết của trường học. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **approvalStatus:** `PENDING` | `APPROVED` | `REJECTED`
                    """
    )
    public ResponseEntity<ResponseDto<SchoolProfileResponse>> getSchoolProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        SchoolProfileResponse response = profileService.getSchoolProfile(principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thành công"));
    }

    @GetMapping("/partnership")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Lấy hồ sơ đối tác đang đăng nhập",
            description = """
                    Trả về thông tin chi tiết của tổ chức đối tác. Chỉ dành cho role **THIRD_PARTY_PARTNERSHIP**.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **partnershipType:** `YOUTH_UNION` | `WARD_GOVERNMENT` | `COMMUNE_GOVERNMENT` | `PUBLIC_ORGANIZATION` | `NGO` | `OTHER`

                    **approvalStatus:** `PENDING` | `APPROVED` | `REJECTED`
                    """
    )
    public ResponseEntity<ResponseDto<PartnershipProfileResponse>> getPartnershipProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        PartnershipProfileResponse response = profileService.getPartnershipProfile(principal.getUser().getId());
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thành công"));
    }

    @PutMapping("/school")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Cập nhật hồ sơ trường học",
            description = """
                    Cập nhật thông tin hồ sơ của trường học đang đăng nhập. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    **Hỗ trợ partial update:** chỉ gửi các trường cần thay đổi.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **schoolType:** `PUBLIC` | `PRIVATE` | `SEMI_PUBLIC`
                    """
    )
    public ResponseEntity<ResponseDto<SchoolProfileResponse>> updateSchoolProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateSchoolProfileRequest request) {
        SchoolProfileResponse response = profileService.updateSchoolProfile(principal.getUser().getId(), request);
        return ResponseEntity.ok(ResponseDto.success(response, "Cập nhật hồ sơ trường học thành công"));
    }

    @PutMapping("/partnership")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Cập nhật hồ sơ đối tác",
            description = """
                    Cập nhật thông tin hồ sơ của tổ chức đối tác đang đăng nhập. Chỉ dành cho role **THIRD_PARTY_PARTNERSHIP**.

                    **Hỗ trợ partial update:** chỉ gửi các trường cần thay đổi.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **partnershipType:** `YOUTH_UNION` | `WARD_GOVERNMENT` | `COMMUNE_GOVERNMENT` | `PUBLIC_ORGANIZATION` | `NGO` | `OTHER`
                    """
    )
    public ResponseEntity<ResponseDto<PartnershipProfileResponse>> updatePartnershipProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePartnershipProfileRequest request) {
        PartnershipProfileResponse response = profileService.updatePartnershipProfile(principal.getUser().getId(), request);
        return ResponseEntity.ok(ResponseDto.success(response, "Cập nhật hồ sơ đối tác thành công"));
    }
}
