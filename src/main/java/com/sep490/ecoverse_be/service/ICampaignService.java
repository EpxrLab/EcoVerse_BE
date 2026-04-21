package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.enums.ParticipationStatus;

import java.util.List;
import java.util.UUID;

public interface ICampaignService {
    CampaignDetailResponse createSchoolCampaign(CreateSchoolCampaignRequest request);
    List<CampaignSummaryResponse> getMySchoolCampaigns();
    CampaignDetailResponse getSchoolCampaignById(UUID campaignId);
    CampaignDetailResponse updateSchoolCampaign(UUID campaignId, UpdateSchoolCampaignRequest request);
    CampaignDetailResponse activateSchoolCampaign(UUID campaignId);
    CampaignDetailResponse setSchoolCampaignDraft(UUID campaignId);
    void replaceAssignedStudentsForSchoolCampaign(UUID campaignId, AssignStudentsRequest request);
    CampaignDetailResponse extendInviting(UUID campaignId, ExtendInvitingRequest request);
    CampaignDetailResponse cancelSchoolCampaign(UUID campaignId);
    void deleteSchoolCampaign(UUID campaignId);

    List<EligibleSchoolResponse> getEligibleSchools();
    CampaignDetailResponse createPartnershipCampaign(CreatePartnershipCampaignRequest request);
    List<CampaignSummaryResponse> getMyPartnershipCampaigns();
    CampaignDetailResponse getPartnershipCampaignById(UUID campaignId);
    CampaignDetailResponse updatePartnershipCampaign(UUID campaignId, CreatePartnershipCampaignRequest request);
    void inviteSchools(UUID campaignId, InviteSchoolsRequest request);
    CampaignDetailResponse activatePartnershipCampaign(UUID campaignId);
    CampaignDetailResponse setPartnershipCampaignDraft(UUID campaignId);
    CampaignDetailResponse cancelPartnershipCampaign(UUID campaignId);
    void deletePartnershipCampaign(UUID campaignId);

    List<PartnershipInvitationSummaryResponse> getPartnershipInvitationSummariesForSchool();

    PartnershipInvitationDetailResponse getPartnershipInvitationDetailForSchool(UUID invitationId);
    void acceptPartnershipInvitation(UUID invitationId);
    void rejectPartnershipInvitation(UUID invitationId);
    PartnershipInvitationAssignedStudentsResponse getAssignedStudentsForPartnershipInvitation(UUID invitationId);
    void replaceAssignedStudentsForPartnershipInvitation(UUID invitationId, AssignStudentsRequest request);

    void updateRoundGameConfig(UUID roundId, UpdateRoundGameConfigRequest request);
    List<PresetAvailableSubCategoriesResponse> getAvailableSubCategoriesForPresets(UUID roundId, UUID gameTypeId, List<UUID> presetIds);
    void bindQuizzesToRound(UUID roundId, List<BindRoundQuizRequest> requests);

    List<CampaignSummaryResponse> getStudentCampaigns(StudentCampaignStatusFilter status);
    CampaignDetailResponse getStudentCampaignDetail(UUID campaignId);
    StudentCurrentRoundContentResponse getStudentCurrentRoundContent(UUID campaignId);
    PlayConfigResponse getPlayConfig(UUID campaignId, UUID roundId);
    List<LeaderboardEntryResponse> getCampaignLeaderboard(UUID campaignId);
    List<LeaderboardEntryResponse> getCampaignRoundLeaderboard(UUID roundId);

    List<ParentCampaignInvitationResponse> getParentCampaignInvitations(ParticipationStatus status);

    /** Lịch sử lời mời / tham gia campaign đã kết thúc (COMPLETED), dùng để tra cứu leaderboard & kết quả. */
    List<ParentCampaignInvitationHistoryResponse> getParentCampaignInvitationHistory(ParticipationStatus status);

    void parentApproveJoin(UUID campaignId, ParentCampaignApprovalRequest request);
    void parentRejectJoin(UUID campaignId, ParentCampaignApprovalRequest request);
    List<CampaignProgressResponse> getParentStudentProgress(UUID studentId);
}

