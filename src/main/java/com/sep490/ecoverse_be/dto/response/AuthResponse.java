package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String id;
    private String email;
    private String userName;
    private String role;
    private String token;
    private String refreshToken;
    boolean isActive;
}
