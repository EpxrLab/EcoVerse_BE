package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.LoginRequest;
import com.sep490.ecoverse_be.dto.request.RefreshTokenRequest;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface IAuthenticationService extends UserDetailsService {
    AuthResponse login(LoginRequest loginRequest);
    void logout (String token, RefreshTokenRequest request);
}