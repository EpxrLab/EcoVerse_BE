package com.sep490.ecoverse_be.dto.response;

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
public class SchoolDetailResponse {

    private UUID id;
    private String userId;
    private String schoolName;
    private SchoolType schoolType;
    private String taxCode;
    private String contactEmail;
    private String phoneNumber;
    private String address;
    private String district;
    private String province;
    private String principalName;
    private String position;
    private String logoUrl;
    private String licenseUrl;
    private ApprovalStatus approvalStatus;
    private LocalDateTime createdAt;
}
