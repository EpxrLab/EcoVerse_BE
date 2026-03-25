package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.request.AdminGameTypeUpsertRequest;
import com.sep490.ecoverse_be.dto.request.AdminWasteItemUpsertRequest;
import com.sep490.ecoverse_be.dto.request.AdminWasteSubCategoryUpsertRequest;
import com.sep490.ecoverse_be.dto.request.MapGameTypeWasteCategoriesRequest;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.AdminCampaignAnalyticsResponse;
import com.sep490.ecoverse_be.dto.response.AdminGameTypeResponse;
import com.sep490.ecoverse_be.dto.response.AdminWasteItemResponse;
import com.sep490.ecoverse_be.dto.response.AdminWasteSubCategoryResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.enums.Role;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;

public interface IAdminService {

    PageResponse<SchoolDetailResponse> getPendingSchools(String keyword, Pageable pageable);

    PageResponse<PartnershipDetailResponse> getPendingPartnerships(String keyword, Pageable pageable);

    PageResponse<SchoolDetailResponse> getApprovedSchools(String keyword, Pageable pageable);

    PageResponse<PartnershipDetailResponse> getApprovedPartnerships(String keyword, Pageable pageable);

    SchoolDetailResponse updateSchoolApproval(UUID id, UpdateApprovalRequest request);

    PartnershipDetailResponse updatePartnershipApproval(UUID id, UpdateApprovalRequest request);

    void updateUserStatus(UUID userId, boolean isActive);

    PageResponse<AdminUserListResponse> getAllUsers(Role role, UUID schoolId, String keyword, Pageable pageable);

    AdminUserListResponse getUserDetail(UUID userId);

    SchoolDetailResponse getSchoolById(UUID schoolId);

    PartnershipDetailResponse getPartnershipById(UUID partnershipId);

    PageResponse<SchoolDetailResponse> getAllSchools(String keyword, Pageable pageable);

    PageResponse<PartnershipDetailResponse> getAllPartnerships(String keyword, Pageable pageable);

    AdminGameTypeResponse createGameType(AdminGameTypeUpsertRequest request);

    AdminGameTypeResponse updateGameType(UUID id, AdminGameTypeUpsertRequest request);

    void deleteGameType(UUID id);

    List<AdminGameTypeResponse> getGameTypes();

    AdminGameTypeResponse getGameTypeById(UUID id);

    AdminGameTypeResponse mapGameTypeWasteCategories(UUID id, MapGameTypeWasteCategoriesRequest request);

    AdminWasteSubCategoryResponse createWasteSubCategory(AdminWasteSubCategoryUpsertRequest request);

    AdminWasteSubCategoryResponse updateWasteSubCategory(UUID id, AdminWasteSubCategoryUpsertRequest request);

    void deleteWasteSubCategory(UUID id);

    List<AdminWasteSubCategoryResponse> getWasteSubCategories();

    AdminWasteItemResponse createWasteItem(AdminWasteItemUpsertRequest request);

    AdminWasteItemResponse updateWasteItem(UUID id, AdminWasteItemUpsertRequest request);

    void deleteWasteItem(UUID id);

    List<AdminWasteItemResponse> getWasteItems();

    AdminWasteItemResponse getWasteItemById(UUID id);

    AdminCampaignAnalyticsResponse getCampaignAnalytics();
}
