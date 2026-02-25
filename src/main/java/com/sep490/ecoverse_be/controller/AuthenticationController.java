package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.LoginRequest;
import com.sep490.ecoverse_be.dto.request.RefreshTokenRequest;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.entity.Account;
import com.sep490.ecoverse_be.service.ITokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.sep490.ecoverse_be.model.UserPrincipal;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private ITokenService tokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<ResponseDto<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        var principal = authentication.getPrincipal();
        Account account = principal.getAccount();

        String accessToken = tokenService.generateToken(account);
        String refreshToken = tokenService.generateRefreshToken(account);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ResponseEntity.ok(ResponseDto.success(authResponse, "Login successful."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = tokenService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ResponseDto.success(authResponse, "Token refreshed successfully."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = tokenService.getToken(authHeader);
        if (accessToken != null) {
            tokenService.invalidateToken(accessToken);
        }

        if (request != null && request.getRefreshToken() != null) {
            tokenService.deleteRefreshToken(request.getRefreshToken());
        }

        return ResponseEntity.ok(ResponseDto.success(null, "Logout successful."));
    }
}
