package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.StudentAccountInfo;
import com.sep490.ecoverse_be.dto.request.ParentCampaignApprovalRequest;
import com.sep490.ecoverse_be.dto.response.CampaignProgressResponse;
import com.sep490.ecoverse_be.dto.response.ParentCampaignInvitationResponse;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.service.ICampaignService;
import com.sep490.ecoverse_be.service.ParentService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parent")
@Tag(name = "Parent")
@PreAuthorize("hasAuthority('PARENT')")
public class ParentController {
    private final ParentService parentService;
    private final ICampaignService campaignService;

    public ParentController(ParentService parentService, ICampaignService campaignService) {
        this.parentService = parentService;
        this.campaignService = campaignService;
    }

    @GetMapping("/children")
    public ResponseEntity<ResponseDto<List<StudentAccountInfo>>> getChildren() {
        List<StudentAccountInfo> studentAccountInfoList = parentService.getChildren();
        return ResponseEntity.ok(ResponseDto.success(studentAccountInfoList, "Danh sách các con của phụ huynh thành công"));
    }

    @GetMapping("/campaign-invitations")
    public ResponseEntity<ResponseDto<List<ParentCampaignInvitationResponse>>> getCampaignInvitations(
            @Parameter(description = "Lọc theo parentApprovalStatus. Bỏ qua = mặc định chỉ lời mời INVITED đã gửi (invitationSentAt), chờ phụ huynh duyệt. "
                    + "Giá trị: PREPARED | INVITED | APPROVED | REJECTED | CANCELLED")
            @RequestParam(required = false) ParticipationStatus status) {
        return ResponseEntity.ok(ResponseDto.success(
                campaignService.getParentCampaignInvitations(status),
                "Danh sách lời mời campaign thành công"));
    }

    @PostMapping("/campaigns/{campaignId}/approve-join")
    public ResponseEntity<ResponseDto<Void>> approveJoin(@PathVariable UUID campaignId,
                                                         @Valid @RequestBody ParentCampaignApprovalRequest request) {
        campaignService.parentApproveJoin(campaignId, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Duyệt tham gia campaign thành công"));
    }

    @PostMapping("/campaigns/{campaignId}/reject-join")
    public ResponseEntity<ResponseDto<Void>> rejectJoin(@PathVariable UUID campaignId,
                                                        @Valid @RequestBody ParentCampaignApprovalRequest request) {
        campaignService.parentRejectJoin(campaignId, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Từ chối tham gia campaign thành công"));
    }

    @GetMapping("/students/{studentId}/campaign-progress")
    public ResponseEntity<ResponseDto<List<CampaignProgressResponse>>> getCampaignProgress(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ResponseDto.success(campaignService.getParentStudentProgress(studentId), "Lấy tiến độ campaign của học sinh thành công"));
    }

}
