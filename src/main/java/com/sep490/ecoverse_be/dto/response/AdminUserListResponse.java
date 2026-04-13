package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.Role;
import lombok.*;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private AdminUserDetail detail;
}
