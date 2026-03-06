package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email/username is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
