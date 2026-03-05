package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.UserResponse;
import com.sep490.ecoverse_be.exception.DisabledException;
import com.sep490.ecoverse_be.exception.DuplicateEntity;
import com.sep490.ecoverse_be.service.IAuthenticationService;
import com.sep490.ecoverse_be.service.ITokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "APIs xác thực, đăng ký, đăng nhập")
public class AuthenticationController {

    @Autowired
    private ITokenService tokenService;

    @Autowired
    private IAuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(
            summary = "Gửi OTP đăng ký tài khoản",
            description = """
                    Gửi mã OTP về email để bắt đầu quá trình đăng ký (dành cho **Trường học** hoặc **Đối tác**).
                    Sau khi nhận OTP, gọi tiếp `/verify-register/school` hoặc `/verify-register/partnership`.

                    **Body:**
                    ```json
                    {
                      "email": "school@example.com"
                    }
                    ```
                    """
    )
    public ResponseDto<String> register(@Valid @RequestBody RegisterRequest user) {
        authenticationService.register(user);
        return new ResponseDto<>(HttpStatus.OK.value(), "Please check mail to get OTP", null);
    }

    @PostMapping("/verify-register/school")
    @Operation(
            summary = "Xác thực OTP và tạo tài khoản Trường học",
            description = """
                    Xác thực mã OTP và tạo tài khoản cho **Trường học** (PARTNERSHIP_SCHOOL).
                    Tài khoản sẽ ở trạng thái **PENDING** chờ Admin duyệt.

                    **Body:**
                    ```json
                    {
                      "schoolName": "Trường Tiểu Học Lê Văn Tám",
                      "contactEmail": "school@example.com",
                      "phoneNumber": "0901234567",
                      "province": "Hồ Chí Minh",
                      "district": "Quận 1",
                      "streetAddress": "123 Lê Lợi",
                      "principalName": "Nguyễn Văn A",
                      "position": "Hiệu trưởng",
                      "taxCode": "0312345678",
                      "linkWeb": "https://school.edu.vn",
                      "description": "Mô tả trường",
                      "schoolType": "PUBLIC",
                      "password": "Pass@1234",
                      "otp": "123456",
                      "logoUrl": "https://cloudinary.com/logo.png",
                      "licenseUrl": "https://cloudinary.com/license.pdf"
                    }
                    ```

                    **schoolType:** `PUBLIC` | `PRIVATE`
                    """
    )
    public ResponseDto<UserResponse> verifyRegisterSchool(@RequestBody @Valid VerifyRegisterSchoolRequest request) {
        try {
            UserResponse userResponse = authenticationService.verifyRegisterSchool(request);
            return new ResponseDto<>(HttpStatus.OK.value(), "Đăng ký thành công, chờ hệ thống xét duyệt", userResponse);
        } catch (IllegalArgumentException e) {
            return new ResponseDto<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        } catch (DuplicateEntity e) {
            return new ResponseDto<>(HttpStatus.CONFLICT.value(), e.getMessage(), null);
        } catch (DataIntegrityViolationException e) {
            return new ResponseDto<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Đã xảy ra lỗi trong quá trình đăng ký, vui lòng thử lại sau.", null);
        }
    }

    @PostMapping("/verify-register/partnership")
    @Operation(
            summary = "Xác thực OTP và tạo tài khoản Đối tác",
            description = """
                    Xác thực mã OTP và tạo tài khoản cho **Đối tác** (THIRD_PARTY_PARTNERSHIP).
                    Tài khoản sẽ ở trạng thái **PENDING** chờ Admin duyệt.

                    **Body:**
                    ```json
                    {
                      "organizationName": "Đoàn Thanh Niên Quận 1",
                      "contactEmail": "partner@example.com",
                      "phoneNumber": "0901234567",
                      "province": "Hồ Chí Minh",
                      "district": "Quận 1",
                      "streetAddress": "456 Nguyễn Huệ",
                      "contactPerson": "Trần Thị B",
                      "position": "Trưởng phòng",
                      "taxCode": "0312345679",
                      "linkWeb": "https://partner.org.vn",
                      "description": "Mô tả tổ chức",
                      "partnershipType": "YOUTH_UNION",
                      "password": "Pass@1234",
                      "otp": "123456",
                      "logoUrl": "https://cloudinary.com/logo.png",
                      "licenseUrl": "https://cloudinary.com/license.pdf"
                    }
                    ```

                    **partnershipType:** `YOUTH_UNION` | `WARD_GOVERNMENT` | `COMMUNE_GOVERNMENT` | `PUBLIC_ORGANIZATION` | `NGO` | `OTHER`
                    """
    )
    public ResponseDto<UserResponse> verifyRegisterPartnership(@RequestBody @Valid VerifyRegisterPartnershipRequest request) {
        try {
            UserResponse userResponse = authenticationService.verifyRegisterPartnership(request);
            return new ResponseDto<>(HttpStatus.OK.value(), "Đăng ký thành công, chờ hệ thống xét duyệt", userResponse);
        } catch (IllegalArgumentException e) {
            return new ResponseDto<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        } catch (DuplicateEntity e) {
            return new ResponseDto<>(HttpStatus.CONFLICT.value(), e.getMessage(), null);
        } catch (DataIntegrityViolationException e) {
            return new ResponseDto<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Đã xảy ra lỗi trong quá trình đăng ký, vui lòng thử lại sau.", null);
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập",
            description = """
                    Đăng nhập cho tất cả loại tài khoản. Trường `email` chấp nhận **email**, **số điện thoại** (phụ huynh), hoặc **student code** (học sinh).

                    **Đăng nhập Trường học / Đối tác (bằng email):**
                    ```json
                    {
                      "email": "school@example.com",
                      "password": "Pass@1234"
                    }
                    ```

                    **Đăng nhập Phụ huynh (bằng số điện thoại):**
                    ```json
                    {
                      "email": "0905324995",
                      "password": "XVsf3pkA"
                    }
                    ```

                    **Đăng nhập Học sinh (bằng student code):**
                    ```json
                    {
                      "email": "AnNHN",
                      "password": "Kp8mVzA1"
                    }
                    ```

                    **Response trả về** `accessToken` và `refreshToken`.
                    """
    )
    public ResponseDto<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authenticationService.login(request);
            return new ResponseDto<>(HttpStatus.OK.value(), "Đăng nhập thành công", authResponse);
        } catch (DisabledException e) {
            return new ResponseDto<>(HttpStatus.FORBIDDEN.value(), e.getMessage(), null);
        } catch (RuntimeException e) {
            return new ResponseDto<>(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), null);
        } catch (Exception e) {
            return new ResponseDto<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null);
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Làm mới Access Token",
            description = """
                    Dùng `refreshToken` để lấy `accessToken` mới khi token cũ hết hạn (60 phút).
                    `refreshToken` có hiệu lực 7 ngày.

                    **Body:**
                    ```json
                    {
                      "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = tokenService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ResponseDto.success(authResponse, "Token refreshed successfully."));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Đăng xuất",
            description = """
                    Vô hiệu hóa `accessToken` hiện tại và xóa `refreshToken` (nếu có).

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Body (tùy chọn):**
                    ```json
                    {
                      "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    ```
                    """
    )
    public ResponseDto<String> logout(
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) RefreshTokenRequest request) {

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authenticationService.logout(token, request);

        return ResponseDto.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đăng xuất thành công")
                .build();
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Quên mật khẩu - Gửi OTP về email",
            description = """
                    Gửi mã OTP về email để đặt lại mật khẩu. Chỉ áp dụng cho tài khoản có email (Trường học, Đối tác, Phụ huynh).

                    **Body:**
                    ```json
                    {
                      "email": "school@example.com"
                    }
                    ```

                    > Luôn trả về 200 dù email có tồn tại hay không (bảo mật).
                    """
    )
    public ResponseDto<Object> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        try {
            authenticationService.forgotPassword(request);
        } catch (Exception ignored) {

        }
        return ResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("Nếu email tồn tại, chúng tôi đã gửi OTP hướng dẫn đặt lại mật khẩu")
                .build();
    }

    @PostMapping("/verify-reset-password")
    @Operation(
            summary = "Xác thực OTP và đặt lại mật khẩu",
            description = """
                    Xác thực OTP và đặt mật khẩu mới.

                    **Body:**
                    ```json
                    {
                      "email": "school@example.com",
                      "otp": "123456",
                      "newPassword": "NewPass@1234"
                    }
                    ```

                    **Yêu cầu mật khẩu:** Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt (`@$!%*?&`).
                    """
    )
    public ResponseDto<Object> verifyResetPassword(@RequestBody @Valid VerifyForgotPasswordRequest request) {

        return ResponseDto.builder()
                .status(HttpStatus.OK.value())
                .data(authenticationService.verifyResetPassword(request))
                .message("Thay đổi mật khẩu thành công!")
                .build();
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT', 'PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Đổi mật khẩu (đã đăng nhập)",
            description = """
                    Đổi mật khẩu khi đã đăng nhập. Yêu cầu Bearer token.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Body:**
                    ```json
                    {
                      "oldPassword": "Pass@1234",
                      "newPassword": "NewPass@5678"
                    }
                    ```

                    **Yêu cầu mật khẩu:** Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt (`@$!%*?&`).
                    """
    )
    public ResponseDto<Object> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authenticationService.changePassword(request);
        return ResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("Đổi mật khẩu thành công")
                .build();
    }
}