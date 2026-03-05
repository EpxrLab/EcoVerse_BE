package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMeResponse {

    private UUID id;
    private String email;
    private String username;
    private String role;
    private String status;
}
