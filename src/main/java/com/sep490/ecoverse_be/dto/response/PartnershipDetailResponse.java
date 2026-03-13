package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.PartnershipType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnershipDetailResponse {

    private UUID id;
    private String userId;
    private String organizationName;
    private PartnershipType partnershipType;
    private String taxCode;
    private String contactEmail;
    private String phoneNumber;
    private String registeredAddress;
    private String geographicScopeWard;
    private String geographicScopeProvince;
    private String contactPerson;
    private String position;
    private String logoUrl;
    private String licenseUrl;
    private ApprovalStatus approvalStatus;
    private LocalDateTime createdAt;
}
