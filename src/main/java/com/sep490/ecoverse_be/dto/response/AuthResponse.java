package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private UUID id;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;
    private Boolean isFirstLogin;
}
