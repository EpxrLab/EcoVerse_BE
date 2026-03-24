package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreatePartnershipCampaignRequest;
import com.sep490.ecoverse_be.dto.request.InviteSchoolsRequest;
import com.sep490.ecoverse_be.dto.response.CampaignDetailResponse;
import com.sep490.ecoverse_be.dto.response.CampaignSummaryResponse;
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
@RequestMapping("/api/partnership")
@PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
public class PartnershipCampaignController {

    @Autowired
    private ICampaignService campaignService;

    @PostMapping("/campaigns")
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
}

