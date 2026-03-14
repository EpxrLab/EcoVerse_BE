package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.SchoolType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchoolDetailResponse implements AdminUserDetail {

    private UUID id;
    private String userId;
    private String schoolName;
    private SchoolType schoolType;
    private String taxCode;
    private String contactEmail;
    private String phoneNumber;
    private String address;
    private String ward;
    private String province;
    private String country;
    private String principalName;
    private String position;
    private String linkWeb;
    private String description;
    private String logoUrl;
    private String licenseUrl;
    private ApprovalStatus approvalStatus;
    private LocalDateTime approvedAt;
    private AccountStatus accountStatus;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
