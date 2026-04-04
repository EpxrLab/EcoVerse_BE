package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.StudentCampaignStatusFilter;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.service.ICampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class StudentCampaignController {

    @Autowired
    private ICampaignService campaignService;

    @GetMapping("/student/campaigns")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ResponseDto<List<CampaignSummaryResponse>>> getStudentCampaigns(
            @RequestParam(required = false) StudentCampaignStatusFilter status) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getStudentCampaigns(status), "Lấy danh sách campaign thành công"));
    }

    @GetMapping("/student/campaigns/{campaignId}")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ResponseDto<CampaignDetailResponse>> getStudentCampaign(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getStudentCampaignDetail(campaignId), "Lấy chi tiết campaign thành công"));
    }

    @GetMapping("/student/campaigns/{campaignId}/current-round-content")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ResponseDto<StudentCurrentRoundContentResponse>> getStudentCurrentRoundContent(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ResponseDto.success(
                campaignService.getStudentCurrentRoundContent(campaignId),
                "Lấy nội dung round hiện tại thành công"
        ));
    }

    @GetMapping("/campaigns/{campaignId}/rounds/{roundId}/play-config")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ResponseDto<PlayConfigResponse>> getPlayConfig(@PathVariable UUID campaignId,
                                                                         @PathVariable UUID roundId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getPlayConfig(campaignId, roundId), "Lấy play config thành công"));
    }

    @GetMapping("/campaigns/{campaignId}/leaderboard")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT', 'PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP', 'ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<List<LeaderboardEntryResponse>>> getCampaignLeaderboard(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getCampaignLeaderboard(campaignId), "Lấy leaderboard campaign thành công"));
    }

    @GetMapping("/campaign-rounds/{roundId}/leaderboard")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT', 'PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP', 'ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<List<LeaderboardEntryResponse>>> getRoundLeaderboard(@PathVariable UUID roundId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getCampaignRoundLeaderboard(roundId), "Lấy leaderboard round thành công"));
    }
}

