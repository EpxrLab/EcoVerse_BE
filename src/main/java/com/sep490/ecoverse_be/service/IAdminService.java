package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.enums.Role;

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

    List<AdminUserListResponse> getAllUsers(Role role, UUID schoolId);

    AdminUserListResponse getUserDetail(UUID userId);

    SchoolDetailResponse getSchoolById(UUID schoolId);

    PartnershipDetailResponse getPartnershipById(UUID partnershipId);

    List<SchoolDetailResponse> getAllSchools();

    List<PartnershipDetailResponse> getAllPartnerships();
}
