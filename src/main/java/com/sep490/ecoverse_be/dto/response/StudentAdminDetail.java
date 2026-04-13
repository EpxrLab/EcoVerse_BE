package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentAdminDetail implements AdminUserDetail {

    private UUID studentId;
    private String fullName;
    private String studentCode;
    private String className;
    private String gradeLevel;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String avatarUrl;
    private String avatarPresignedUrl;
    private AccountStatus accountStatus;
    private Boolean isActive;
    private String schoolName;
    private UUID schoolId;
}
