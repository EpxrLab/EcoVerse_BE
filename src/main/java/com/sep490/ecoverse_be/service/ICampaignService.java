package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface ICampaignService {
    CampaignDetailResponse createSchoolCampaign(SchoolCampaignUpsertRequest request);
    List<CampaignSummaryResponse> getMySchoolCampaigns();
    CampaignDetailResponse getSchoolCampaignById(UUID campaignId);
    CampaignDetailResponse updateSchoolCampaign(UUID campaignId, SchoolCampaignUpsertRequest request);
    CampaignDetailResponse activateSchoolCampaign(UUID campaignId);
    CampaignDetailResponse setSchoolCampaignDraft(UUID campaignId);
    void inviteStudentsToSchoolCampaign(UUID campaignId, AssignStudentsRequest request);
    CampaignDetailResponse extendInviting(UUID campaignId, ExtendInvitingRequest request);
    CampaignDetailResponse cancelSchoolCampaign(UUID campaignId);

    CampaignDetailResponse createPartnershipCampaign(CreatePartnershipCampaignRequest request);
    List<CampaignSummaryResponse> getMyPartnershipCampaigns();
    CampaignDetailResponse getPartnershipCampaignById(UUID campaignId);
    CampaignDetailResponse updatePartnershipCampaign(UUID campaignId, CreatePartnershipCampaignRequest request);
    void inviteSchools(UUID campaignId, InviteSchoolsRequest request);
    CampaignDetailResponse activatePartnershipCampaign(UUID campaignId);
    CampaignDetailResponse setPartnershipCampaignDraft(UUID campaignId);
    CampaignDetailResponse cancelPartnershipCampaign(UUID campaignId);

    List<PartnershipInvitationResponse> getPartnershipInvitationsForSchool();
    void acceptPartnershipInvitation(UUID invitationId);
    void rejectPartnershipInvitation(UUID invitationId);
    void assignStudentsToPartnershipInvitation(UUID invitationId, AssignStudentsRequest request);

    void updateRoundGameConfig(UUID roundId, UpdateRoundGameConfigRequest request);
    List<PresetAvailableSubCategoriesResponse> getAvailableSubCategoriesForPresets(UUID roundId, UUID gameTypeId, List<UUID> presetIds);
    void bindQuizzesToRound(UUID roundId, List<BindRoundQuizRequest> requests);

    List<CampaignSummaryResponse> getStudentCampaigns(StudentCampaignStatusFilter status);
    CampaignDetailResponse getStudentCampaignDetail(UUID campaignId);
    PlayConfigResponse getPlayConfig(UUID campaignId, UUID roundId);
    List<LeaderboardEntryResponse> getCampaignLeaderboard(UUID campaignId);
    List<LeaderboardEntryResponse> getCampaignRoundLeaderboard(UUID roundId);

    List<ParentCampaignInvitationResponse> getParentCampaignInvitations();
    void parentApproveJoin(UUID campaignId, ParentCampaignApprovalRequest request);
    void parentRejectJoin(UUID campaignId, ParentCampaignApprovalRequest request);
    List<CampaignProgressResponse> getParentStudentProgress(UUID studentId);
}

