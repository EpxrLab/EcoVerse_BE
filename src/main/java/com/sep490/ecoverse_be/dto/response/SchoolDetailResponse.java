package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.SchoolType;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
    private String logoPresignedUrl;
    private String licenseUrl;
    private String licensePresignedUrl;
    private ApprovalStatus approvalStatus;
    private OffsetDateTime approvedAt;
    private AccountStatus accountStatus;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
