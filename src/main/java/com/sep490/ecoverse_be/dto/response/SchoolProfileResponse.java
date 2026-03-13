package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchoolProfileResponse {

    private UUID id;
    private String schoolName;
    private String schoolType;
    private String taxCode;
    private String address;
    private String ward;
    private String province;
    private String phoneNumber;
    private String principalName;
    private String position;
    private String contactEmail;
    private String linkWeb;
    private String description;
    private String approvalStatus;
    private String logoUrl;
    private String licenseUrl;
}
