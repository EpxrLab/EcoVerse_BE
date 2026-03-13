package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnershipProfileResponse {

    private UUID id;
    private String organizationName;
    private String partnershipType;
    private String contactEmail;
    private String phoneNumber;
    private String registeredAddress;
    private String geographicScopeWard;
    private String geographicScopeProvince;
    private String contactPerson;
    private String position;
    private String taxCode;
    private String linkWeb;
    private String description;
    private String approvalStatus;
    private String logoUrl;
    private String licenseUrl;
}
