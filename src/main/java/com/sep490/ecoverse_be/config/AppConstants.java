package com.sep490.ecoverse_be.config;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String[] PUBLIC_URLS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/verify-reset-password",
            "/api/loginByGoogle",
            "/oauth2/authorization/**",
            "/login/oauth2/code/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/api/otp/verify-register",
            "/api/otp/verify-reset-password",
            "/api/payments/webhook/payos",
            "/api/payments/cancel/**",
            "/api/locations/provinces",
            "/api/locations/wards/**",
            "/api/auth/verify-register/school",
            "/api/auth/verify-register/partnership",
            "/api/auth/verify-otp",
            "/api/subscription-plans",
            "/api/subscription-plans/**",
            "/api/files/upload/contract",
            "/api/files/view/**",
            // Debug endpoints
            "/api/debug/**",
            // WebSocket: HTTP handshake bo qua Spring Security, xac thuc xu ly o STOMP layer
            "/ws/**"
    };
}
