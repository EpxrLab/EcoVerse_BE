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

    // Thông tin User chung
    private UUID userId;
    private UUID schoolId;
    private UUID partnerId;
    private String email;
    private String username;
    private Role role;
    private AccountStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Tên hiển thị: schoolName / organizationName / fullName tùy theo role
    private String displayName;

    // Trường học mà học sinh / phụ huynh thuộc về (chỉ dành cho STUDENT và PARENT)
    private String schoolName;

    // Chỉ dành cho STUDENT
    private String studentCode;
    private String className;
    private String gradeLevel;

    // Chỉ dành cho PARENT
    private String phoneNumber;
}
