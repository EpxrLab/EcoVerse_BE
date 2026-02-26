package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.LoginRequest;
import com.sep490.ecoverse_be.dto.request.RefreshTokenRequest;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.exception.DisabledException;
import com.sep490.ecoverse_be.service.IAuthenticationService;
import com.sep490.ecoverse_be.service.ITokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private ITokenService tokenService;

    @Autowired
    private IAuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseDto<AuthResponse> loginForSchoolAndPartnerShip(@Valid @RequestBody LoginRequest request) {
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
    public ResponseEntity<ResponseDto<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = tokenService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ResponseDto.success(authResponse, "Token refreshed successfully."));
    }

    @PostMapping("/logout")
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
}
