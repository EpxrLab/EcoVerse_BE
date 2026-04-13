package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreatePartnershipCampaignRequest;
import com.sep490.ecoverse_be.dto.request.InviteSchoolsRequest;
import com.sep490.ecoverse_be.dto.response.CampaignDetailResponse;
import com.sep490.ecoverse_be.dto.response.CampaignRewardResponse;
import com.sep490.ecoverse_be.dto.response.CampaignSummaryResponse;
import com.sep490.ecoverse_be.dto.response.EligibleSchoolResponse;
import com.sep490.ecoverse_be.dto.response.LeaderboardEntryResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.ICampaignRewardService;
import com.sep490.ecoverse_be.service.ICampaignService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partnership")
@PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
public class PartnershipCampaignController {

    @Autowired
    private ICampaignService campaignService;

    @Autowired
    private ICampaignRewardService campaignRewardService;

    @GetMapping("/campaigns/eligible-schools")
    @Operation(
            summary = "Lấy danh sách trường đủ điều kiện mời",
            description = "Trả về các trường có cùng ward (ưu tiên) hoặc cùng province với khu vực hoạt động đã đăng ký của partnership."
    )
    public ResponseEntity<ResponseDto<List<EligibleSchoolResponse>>> getEligibleSchools() {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getEligibleSchools(), "Lấy danh sách trường thành công"));
    }

    @PostMapping("/campaigns")
    @Operation(summary = "Tạo partnership campaign", description = "Tạo campaign và mời trường ngay trong cùng request nếu truyền `schoolIds`.")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> createCampaign(@Valid @RequestBody CreatePartnershipCampaignRequest request) {
        return ResponseEntity.status(201).body(ResponseDto.created(campaignService.createPartnershipCampaign(request), "Tạo campaign thành công"));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ResponseDto<List<CampaignSummaryResponse>>> getMyCampaigns() {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getMyPartnershipCampaigns(), "Lấy danh sách campaign thành công"));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> getCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getPartnershipCampaignById(id), "Lấy chi tiết campaign thành công"));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> updateCampaign(@PathVariable UUID id,
                                                                               @Valid @RequestBody CreatePartnershipCampaignRequest request) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.updatePartnershipCampaign(id, request), "Cập nhật campaign thành công"));
    }

    @PostMapping("/campaigns/{id}/invite-schools")
    public ResponseEntity<ResponseDto<Void>> inviteSchools(@PathVariable UUID id,
                                                           @Valid @RequestBody InviteSchoolsRequest request) {
        campaignService.inviteSchools(id, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Đã gửi lời mời đến school"));
    }

    @PutMapping("/campaigns/{id}/activate")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> activateCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.activatePartnershipCampaign(id), "Kích hoạt campaign thành công"));
    }

    @PutMapping("/campaigns/{id}/set-draft")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> setDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.setPartnershipCampaignDraft(id), "Chuyển campaign về DRAFT thành công"));
    }

    @PutMapping("/campaigns/{id}/cancel")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> cancelCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.cancelPartnershipCampaign(id), "Hủy campaign thành công"));
    }

    @DeleteMapping("/campaigns/{id}")
    @Operation(
            summary = "Xóa partnership campaign (soft delete)",
            description = "Chỉ xóa được khi campaign đang ở trạng thái **DRAFT**. Dữ liệu không bị xóa vật lý."
    )
    public ResponseEntity<ResponseDto<Void>> deleteCampaign(@PathVariable UUID id) {
        campaignService.deletePartnershipCampaign(id);
        return ResponseEntity.ok(ResponseDto.success(null, "Xóa campaign thành công"));
    }

    @GetMapping("/campaigns/{id}/rewards")
    @Operation(
            summary = "Xem danh sách quà thưởng của campaign",
            description = "Trả về danh sách rewards đã cấu hình, sắp xếp theo rankPosition tăng dần."
    )
    public ResponseEntity<ResponseDto<List<CampaignRewardResponse>>> getRewards(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignRewardService.getRewards(id), "Lấy danh sách quà thưởng thành công"));
    }

    @GetMapping("/campaigns/{id}/leaderboard")
    @Operation(summary = "Xem leaderboard của partnership campaign")
    public ResponseEntity<ResponseDto<List<LeaderboardEntryResponse>>> getCampaignLeaderboard(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getCampaignLeaderboard(id), "Lấy leaderboard campaign thành công"));
    }

    @GetMapping("/campaigns/{id}/rounds/{roundId}/leaderboard")
    @Operation(summary = "Xem leaderboard theo round của partnership campaign")
    public ResponseEntity<ResponseDto<List<LeaderboardEntryResponse>>> getRoundLeaderboard(@PathVariable UUID id,
                                                                                           @PathVariable UUID roundId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getCampaignRoundLeaderboard(roundId), "Lấy leaderboard round thành công"));
    }
}

