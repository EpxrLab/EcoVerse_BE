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

    // Lấy danh sách tất cả user theo role (null = tất cả role)
    // Khi role là STUDENT hoặc PARENT, có thể lọc thêm theo schoolId
    List<AdminUserListResponse> getAllUsers(Role role, UUID schoolId);

    // Lấy chi tiết một user theo userId
    AdminUserListResponse getUserDetail(UUID userId);

    // Lấy chi tiết một trường học theo school entity id
    SchoolDetailResponse getSchoolById(UUID schoolId);

    // Lấy chi tiết một đối tác theo partnership entity id
    PartnershipDetailResponse getPartnershipById(UUID partnershipId);
}
