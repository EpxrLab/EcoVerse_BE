package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.dto.response.UserResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface IAuthenticationService extends UserDetailsService {
    void register(RegisterRequest registerRequest);
    UserResponse verifyRegisterSchool(VerifyRegisterSchoolRequest registerRequest);
    void verifyOtpOrThrow(String email, String otp);
    UserResponse verifyRegisterPartnership(VerifyRegisterPartnershipRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    void logout(String token, RefreshTokenRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    AuthResponse verifyResetPassword(VerifyForgotPasswordRequest request);
    void changePassword(ChangePasswordRequest request);
}
