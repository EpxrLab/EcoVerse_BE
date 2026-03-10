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
import org.springframework.http.HttpStatus;
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

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy thông tin thành công",
                      "data": {
                        "id": "805fbb74-0b23-44c6-8f1a-b5478df61d4d",
                        "email": null,
                        "username": "AnNHN",
                        "role": "STUDENT",
                        "status": "ACTIVE"
                      }
                    }
                    ```

                    - `email`: null nếu là tài khoản Học sinh
                    - `username`: studentCode (học sinh) hoặc số điện thoại (phụ huynh), null nếu là Trường/Đối tác
                    """
    )
    public ResponseDto<UserMeResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserMeResponse response = profileService.getMe(principal.getUser().getId());
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy thông tin thành công", response);
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

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy thông tin thành công",
                      "data": {
                        "id": "uuid",
                        "studentCode": "AnNHN",
                        "fullName": "Nguyễn Hoàng Nhật Ân",
                        "className": "5A",
                        "gradeLevel": "5",
                        "dateOfBirth": "2015-03-20",
                        "gender": "MALE",
                        "avatarUrl": null,
                        "totalCoins": "0.00",
                        "school": {
                          "id": "uuid",
                          "schoolName": "Trường Tiểu Học Lê Văn Tám"
                        }
                      }
                    }
                    ```
                    """
    )
    public ResponseDto<StudentProfileResponse> getStudentProfile(@AuthenticationPrincipal UserPrincipal principal) {
        StudentProfileResponse response = profileService.getStudentProfile(principal.getUser().getId());
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy thông tin thành công", response);
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

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy thông tin thành công",
                      "data": {
                        "id": "uuid",
                        "fullName": "Nguyễn Văn A",
                        "phoneNumber": "0905324995",
                        "email": "parent@gmail.com",
                        "children": [
                          {
                            "id": "uuid",
                            "studentCode": "AnNHN",
                            "fullName": "Nguyễn Hoàng Nhật Ân",
                            "className": "5A",
                            "gradeLevel": "5",
                            "schoolName": "Trường Tiểu Học Lê Văn Tám"
                          }
                        ]
                      }
                    }
                    ```
                    """
    )
    public ResponseDto<ParentProfileResponse> getParentProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ParentProfileResponse response = profileService.getParentProfile(principal.getUser().getId());
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy thông tin thành công", response);
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

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy thông tin thành công",
                      "data": {
                        "id": "uuid",
                        "schoolName": "Trường Tiểu Học Lê Văn Tám",
                        "schoolType": "PUBLIC",
                        "taxCode": "0312345678",
                        "address": "123 Lê Lợi",
                        "district": "Quận 1",
                        "province": "Hồ Chí Minh",
                        "phoneNumber": "0901234567",
                        "principalName": "Nguyễn Văn A",
                        "position": "Hiệu trưởng",
                        "contactEmail": "school@example.com",
                        "linkWeb": "https://school.edu.vn",
                        "description": "Mô tả trường",
                        "approvalStatus": "APPROVED",
                        "logoUrl": "https://cloudinary.com/logo.png",
                        "licenseUrl": "https://cloudinary.com/license.pdf"
                      }
                    }
                    ```

                    **approvalStatus:** `PENDING` | `APPROVED` | `REJECTED`
                    """
    )
    public ResponseDto<SchoolProfileResponse> getSchoolProfile(@AuthenticationPrincipal UserPrincipal principal) {
        SchoolProfileResponse response = profileService.getSchoolProfile(principal.getUser().getId());
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy thông tin thành công", response);
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

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy thông tin thành công",
                      "data": {
                        "id": "uuid",
                        "organizationName": "Đoàn Thanh Niên Quận 1",
                        "partnershipType": "YOUTH_UNION",
                        "contactEmail": "partner@example.com",
                        "phoneNumber": "0901234567",
                        "registeredAddress": "456 Nguyễn Huệ",
                        "geographicScopeDistrict": "Quận 1",
                        "geographicScopeProvince": "Hồ Chí Minh",
                        "contactPerson": "Trần Thị B",
                        "position": "Trưởng phòng",
                        "taxCode": "0312345679",
                        "linkWeb": "https://partner.org.vn",
                        "description": "Mô tả tổ chức",
                        "approvalStatus": "APPROVED",
                        "logoUrl": "https://cloudinary.com/logo.png",
                        "licenseUrl": "https://cloudinary.com/license.pdf"
                      }
                    }
                    ```

                    **partnershipType:** `YOUTH_UNION` | `WARD_GOVERNMENT` | `COMMUNE_GOVERNMENT` | `PUBLIC_ORGANIZATION` | `NGO` | `OTHER`

                    **approvalStatus:** `PENDING` | `APPROVED` | `REJECTED`
                    """
    )
    public ResponseDto<PartnershipProfileResponse> getPartnershipProfile(@AuthenticationPrincipal UserPrincipal principal) {
        PartnershipProfileResponse response = profileService.getPartnershipProfile(principal.getUser().getId());
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy thông tin thành công", response);
    }

    @PutMapping("/school")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Cập nhật hồ sơ trường học",
            description = """
                    Cập nhật thông tin hồ sơ của trường học đang đăng nhập. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    **Các trường ĐƯỢC phép cập nhật:**
                    `schoolName`, `schoolType`, `address`, `district`, `province`, `phoneNumber`,
                    `principalName`, `position`, `contactEmail`, `linkWeb`, `description`

                    **Các trường KHÔNG được phép cập nhật qua API này:**
                    - `taxCode` — bất biến sau khi đăng ký
                    - `logoUrl` — cập nhật qua API upload logo riêng
                    - `licenseUrl` — cập nhật qua API upload giấy phép riêng

                    **Hỗ trợ partial update:** chỉ gửi các trường cần thay đổi, các trường không gửi sẽ giữ nguyên.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body mẫu:**
                    ```json
                    {
                      "schoolName": "Trường Tiểu Học Lê Văn Tám",
                      "schoolType": "PUBLIC",
                      "address": "123 Lê Lợi",
                      "district": "Quận 1",
                      "province": "Hồ Chí Minh",
                      "phoneNumber": "0901234567",
                      "principalName": "Nguyễn Văn B",
                      "position": "Hiệu trưởng",
                      "contactEmail": "school@example.com",
                      "linkWeb": "https://school.edu.vn",
                      "description": "Mô tả mới"
                    }
                    ```

                    **schoolType:** `PUBLIC` | `PRIVATE` | `SEMI_PUBLIC`
                    """
    )
    public ResponseDto<SchoolProfileResponse> updateSchoolProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateSchoolProfileRequest request) {
        SchoolProfileResponse response = profileService.updateSchoolProfile(principal.getUser().getId(), request);
        return new ResponseDto<>(HttpStatus.OK.value(), "Cập nhật hồ sơ trường học thành công", response);
    }

    @PutMapping("/partnership")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Cập nhật hồ sơ đối tác",
            description = """
                    Cập nhật thông tin hồ sơ của tổ chức đối tác đang đăng nhập. Chỉ dành cho role **THIRD_PARTY_PARTNERSHIP**.

                    **Các trường ĐƯỢC phép cập nhật:**
                    `organizationName`, `partnershipType`, `contactEmail`, `phoneNumber`, `registeredAddress`,
                    `geographicScopeDistrict`, `geographicScopeProvince`, `contactPerson`, `position`, `linkWeb`, `description`

                    **Các trường KHÔNG được phép cập nhật qua API này:**
                    - `taxCode` — bất biến sau khi đăng ký
                    - `logoUrl` — cập nhật qua API upload logo riêng
                    - `licenseUrl` — cập nhật qua API upload giấy phép riêng

                    **Hỗ trợ partial update:** chỉ gửi các trường cần thay đổi, các trường không gửi sẽ giữ nguyên.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body mẫu:**
                    ```json
                    {
                      "organizationName": "Đoàn Thanh Niên Quận 1",
                      "partnershipType": "YOUTH_UNION",
                      "contactEmail": "partner@example.com",
                      "phoneNumber": "0901234567",
                      "registeredAddress": "456 Nguyễn Huệ",
                      "geographicScopeDistrict": "Quận 1",
                      "geographicScopeProvince": "Hồ Chí Minh",
                      "contactPerson": "Trần Thị C",
                      "position": "Phó phòng",
                      "linkWeb": "https://partner.org.vn",
                      "description": "Mô tả mới"
                    }
                    ```

                    **partnershipType:** `YOUTH_UNION` | `WARD_GOVERNMENT` | `COMMUNE_GOVERNMENT` | `PUBLIC_ORGANIZATION` | `NGO` | `OTHER`
                    """
    )
    public ResponseDto<PartnershipProfileResponse> updatePartnershipProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePartnershipProfileRequest request) {
        PartnershipProfileResponse response = profileService.updatePartnershipProfile(principal.getUser().getId(), request);
        return new ResponseDto<>(HttpStatus.OK.value(), "Cập nhật hồ sơ đối tác thành công", response);
    }
}
