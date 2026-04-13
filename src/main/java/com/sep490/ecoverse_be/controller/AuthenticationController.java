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
    public ResponseEntity<ResponseDto<String>> register(@Valid @RequestBody RegisterRequest user) {
        authenticationService.register(user);
        return ResponseEntity.ok(ResponseDto.success(null, "Please check mail to get OTP"));
    }

    @PostMapping("/verify-register/school")
    @Operation(
            summary = "Tạo tài khoản Trường học (sau khi đã xác thực OTP)",
            description = """
                    Tạo tài khoản cho **Trường học** (PARTNERSHIP_SCHOOL).
                    Tài khoản sẽ ở trạng thái **PENDING** chờ Admin duyệt.

                    > **Yêu cầu:** Phải gọi `/verify-otp` thành công trước với email tương ứng.
                    > OTP verified có hiệu lực trong **15 phút**.

                    **Luồng đúng:**
                    1. `POST /register` → nhận OTP qua email
                    2. `POST /verify-otp` → xác thực OTP
                    3. `POST /verify-register/school` → điền thông tin và tạo tài khoản

                    **schoolType:** `PUBLIC` | `PRIVATE`

                    **Lỗi có thể xảy ra:**
                    - `400` — Email chưa xác thực OTP hoặc OTP đã hết hạn 15 phút
                    - `409` — Email đã tồn tại trong hệ thống
                    """
    )
    public ResponseEntity<ResponseDto<UserResponse>> verifyRegisterSchool(
            @RequestBody @Valid VerifyRegisterSchoolRequest request) {
        try {
            UserResponse userResponse = authenticationService.verifyRegisterSchool(request);
            return ResponseEntity.ok(
                    ResponseDto.success(userResponse, "Đăng ký thành công, chờ hệ thống xét duyệt"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDto.badRequest(null, e.getMessage()));
        } catch (DuplicateEntity e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseDto.badRequest(null, e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("Đã xảy ra lỗi trong quá trình đăng ký, vui lòng thử lại sau."));
        }
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Xác thực OTP",
            description = """
                    Xác thực mã OTP
                    **Body:**
                    ```json
                    {
                      "email": "abc@gmail.com"
                      "otp": "123456"
                    }
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<Void>> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        authenticationService.verifyOtpOrThrow(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ResponseDto.success(null, "Xác thực otp thành công"));
    }

    @PostMapping("/verify-register/partnership")
    @Operation(
            summary = "Xác thực OTP và tạo tài khoản Đối tác",
            description = """
                    Xác thực mã OTP và tạo tài khoản cho **Đối tác** (THIRD_PARTY_PARTNERSHIP).
                    Tài khoản sẽ ở trạng thái **PENDING** chờ Admin duyệt.

                    **partnershipType:** `YOUTH_UNION` | `WARD_GOVERNMENT` | `COMMUNE_GOVERNMENT` | `PUBLIC_ORGANIZATION` | `NGO` | `OTHER`
                    """
    )
    public ResponseEntity<ResponseDto<UserResponse>> verifyRegisterPartnership(
            @RequestBody @Valid VerifyRegisterPartnershipRequest request) {
        try {
            UserResponse userResponse = authenticationService.verifyRegisterPartnership(request);
            return ResponseEntity.ok(
                    ResponseDto.success(userResponse, "Đăng ký thành công, chờ hệ thống xét duyệt"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDto.badRequest(null, e.getMessage()));
        } catch (DuplicateEntity e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseDto.badRequest(null, e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("Đã xảy ra lỗi trong quá trình đăng ký, vui lòng thử lại sau."));
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập",
            description = """
                    Đăng nhập cho tất cả loại tài khoản. Trường `email` chấp nhận **email**, **số điện thoại** (phụ huynh), hoặc **student code** (học sinh).

                    **Response trả về** `accessToken` và `refreshToken`.
                    """
    )
    public ResponseEntity<ResponseDto<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authenticationService.login(request);
            return ResponseEntity.ok(ResponseDto.success(authResponse, "Đăng nhập thành công"));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDto.forbidden(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.unauthorized(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error(e.getMessage()));
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
                    """
    )
    public ResponseEntity<ResponseDto<String>> logout(
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) RefreshTokenRequest request) {

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authenticationService.logout(token, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Đăng xuất thành công"));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Quên mật khẩu - Gửi OTP về email",
            description = """
                    Gửi mã OTP về email để đặt lại mật khẩu. Chỉ áp dụng cho tài khoản có email.

                    > Luôn trả về 200 dù email có tồn tại hay không (bảo mật).
                    """
    )
    public ResponseEntity<ResponseDto<Object>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        try {
            authenticationService.forgotPassword(request);
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok(
                ResponseDto.success(null, "Nếu email tồn tại, chúng tôi đã gửi OTP hướng dẫn đặt lại mật khẩu"));
    }

    @PostMapping("/verify-reset-password")
    @Operation(
            summary = "Xác thực OTP và đặt lại mật khẩu",
            description = """
                    Xác thực OTP và đặt mật khẩu mới.

                    **Yêu cầu mật khẩu:** Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt (`@$!%*?&`).
                    """
    )
    public ResponseEntity<ResponseDto<Object>> verifyResetPassword(
            @RequestBody @Valid VerifyForgotPasswordRequest request) {
        return ResponseEntity.ok(
                ResponseDto.success(authenticationService.verifyResetPassword(request), "Thay đổi mật khẩu thành công!"));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT', 'PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
    @Operation(
            summary = "Đổi mật khẩu (đã đăng nhập)",
            description = """
                    Đổi mật khẩu khi đã đăng nhập. Yêu cầu Bearer token.

                    **Yêu cầu mật khẩu:** Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt (`@$!%*?&`).
                    """
    )
    public ResponseEntity<ResponseDto<Object>> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authenticationService.changePassword(request);
        return ResponseEntity.ok(ResponseDto.success(null, "Đổi mật khẩu thành công"));
    }
}
