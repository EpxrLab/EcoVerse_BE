package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.CampaignDetailResponse;
import com.sep490.ecoverse_be.dto.response.CampaignSummaryResponse;
import com.sep490.ecoverse_be.dto.response.LeaderboardEntryResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipInvitationAssignedStudentsResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipInvitationDetailResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipInvitationSummaryResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.ICampaignService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/school")
@PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
public class SchoolCampaignController {

    @Autowired
    private ICampaignService campaignService;

    @PostMapping("/campaigns")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> createCampaign(@Valid @RequestBody CreateSchoolCampaignRequest request) {
        return ResponseEntity.status(201).body(ResponseDto.created(campaignService.createSchoolCampaign(request), "Tạo campaign thành công"));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ResponseDto<List<CampaignSummaryResponse>>> getMyCampaigns() {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getMySchoolCampaigns(), "Lấy danh sách campaign thành công"));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> getCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getSchoolCampaignById(id), "Lấy chi tiết campaign thành công"));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> updateCampaign(@PathVariable UUID id,
                                                                               @Valid @RequestBody UpdateSchoolCampaignRequest request) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.updateSchoolCampaign(id, request), "Cập nhật campaign thành công"));
    }

    @PutMapping("/campaigns/{id}/activate")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> activateCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.activateSchoolCampaign(id), "Kích hoạt campaign thành công"));
    }

    @PutMapping("/campaigns/{id}/set-draft")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> setDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.setSchoolCampaignDraft(id), "Chuyển campaign về DRAFT thành công"));
    }

    @PutMapping("/campaigns/{id}/assigned-students")
    public ResponseEntity<ResponseDto<Void>> replaceSchoolCampaignStudents(@PathVariable UUID id,
                                                                           @Valid @RequestBody AssignStudentsRequest request) {
        campaignService.replaceAssignedStudentsForSchoolCampaign(id, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Cập nhật danh sách học sinh tham gia thành công"));
    }

    @PutMapping("/campaigns/{id}/extend-inviting")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> extendInviting(@PathVariable UUID id,
                                                                               @Valid @RequestBody ExtendInvitingRequest request) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.extendInviting(id, request), "Gia hạn thời gian mời thành công"));
    }

    @PutMapping("/campaigns/{id}/cancel")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> cancelCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.cancelSchoolCampaign(id), "Hủy campaign thành công"));
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteCampaign(@PathVariable UUID id) {
        campaignService.deleteSchoolCampaign(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Xóa campaign thành công"));
    }

    @GetMapping("/partnership-invitations")
    public ResponseEntity<ResponseDto<List<PartnershipInvitationSummaryResponse>>> getPartnershipInvitationSummaries() {
        return ResponseEntity.ok(ResponseDto.success(
                campaignService.getPartnershipInvitationSummariesForSchool(),
                "Lấy danh sách lời mời thành công"));
    }

    @GetMapping("/partnership-invitations/{id}")
    public ResponseEntity<ResponseDto<PartnershipInvitationDetailResponse>> getPartnershipInvitationDetail(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(
                campaignService.getPartnershipInvitationDetailForSchool(id),
                "Lấy chi tiết lời mời thành công"));
    }

    @PutMapping("/partnership-invitations/{id}/accept")
    public ResponseEntity<ResponseDto<Void>> acceptInvitation(@PathVariable UUID id) {
        campaignService.acceptPartnershipInvitation(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Đã chấp nhận lời mời"));
    }

    @PutMapping("/partnership-invitations/{id}/reject")
    public ResponseEntity<ResponseDto<Void>> rejectInvitation(@PathVariable UUID id) {
        campaignService.rejectPartnershipInvitation(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Đã từ chối lời mời"));
    }

    @GetMapping("/partnership-invitations/{id}/assigned-students")
    public ResponseEntity<ResponseDto<PartnershipInvitationAssignedStudentsResponse>> getAssignedStudents(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(
                campaignService.getAssignedStudentsForPartnershipInvitation(id),
                "Lấy danh sách học sinh đã chọn thành công"));
    }

    @PutMapping("/partnership-invitations/{id}/assigned-students")
    public ResponseEntity<ResponseDto<Void>> replaceAssignedStudents(@PathVariable UUID id,
                                                                     @Valid @RequestBody AssignStudentsRequest request) {
        campaignService.replaceAssignedStudentsForPartnershipInvitation(id, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Cập nhật danh sách học sinh thành công"));
    }

    @GetMapping("/campaigns/{id}/leaderboard")
    public ResponseEntity<ResponseDto<List<LeaderboardEntryResponse>>> getCampaignLeaderboard(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getCampaignLeaderboard(id), "Lấy leaderboard campaign thành công"));
    }

    @GetMapping("/campaigns/{id}/rounds/{roundId}/leaderboard")
    public ResponseEntity<ResponseDto<List<LeaderboardEntryResponse>>> getRoundLeaderboard(@PathVariable UUID id,
                                                                                           @PathVariable UUID roundId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getCampaignRoundLeaderboard(roundId), "Lấy leaderboard round thành công"));
    }
}

