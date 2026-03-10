package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;

import java.util.List;
import java.util.UUID;

public interface IAdminService {

    List<SchoolDetailResponse> getPendingSchools();

    List<PartnershipDetailResponse> getPendingPartnerships();

    List<SchoolDetailResponse> getApprovedSchools();

    List<PartnershipDetailResponse> getApprovedPartnerships();

    SchoolDetailResponse updateSchoolApproval(UUID id, UpdateApprovalRequest request);

    PartnershipDetailResponse updatePartnershipApproval(UUID id, UpdateApprovalRequest request);

    void updateUserStatus(UUID userId, boolean isActive);
}
