package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.Role;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserListResponse {

    private UUID userId;
    private String email;
    private String username;
    private Role role;
    private AccountStatus status;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private AdminUserDetail detail;
}
