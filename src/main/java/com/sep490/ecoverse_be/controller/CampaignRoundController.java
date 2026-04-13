package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.BindRoundQuizRequest;
import com.sep490.ecoverse_be.dto.request.UpdateRoundGameConfigRequest;
import com.sep490.ecoverse_be.dto.response.PresetAvailableSubCategoriesResponse;
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
@RequestMapping("/api/campaign-rounds")
@PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
public class CampaignRoundController {

    @Autowired
    private ICampaignService campaignService;

    @PutMapping("/{id}/game-config")
    public ResponseEntity<ResponseDto<Void>> updateGameConfig(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateRoundGameConfigRequest request) {
        campaignService.updateRoundGameConfig(id, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Cập nhật game config thành công"));
    }

    @GetMapping("/{id}/game-config/available-sub-categories")
    public ResponseEntity<ResponseDto<List<PresetAvailableSubCategoriesResponse>>> getAvailableSubCategories(
            @PathVariable UUID id,
            @RequestParam UUID gameTypeId,
            @RequestParam List<UUID> presetIds) {
        return ResponseEntity.ok(ResponseDto.success(
                campaignService.getAvailableSubCategoriesForPresets(id, gameTypeId, presetIds),
                "Lấy danh sách sub-category khả dụng theo preset thành công"
        ));
    }

    // Gắn nhiều quiz vào round cùng lúc (POST để set mới, PUT để ghi đè toàn bộ)
    @PostMapping("/{id}/quizzes/bind")
    public ResponseEntity<ResponseDto<Void>> bindQuizzes(@PathVariable UUID id,
                                                         @Valid @RequestBody List<BindRoundQuizRequest> requests) {
        campaignService.bindQuizzesToRound(id, requests);
        return ResponseEntity.ok(ResponseDto.success(null, "Gắn danh sách quiz cho round thành công"));
    }

    @PutMapping("/{id}/quizzes/bind")
    public ResponseEntity<ResponseDto<Void>> rebindQuizzes(@PathVariable UUID id,
                                                           @Valid @RequestBody List<BindRoundQuizRequest> requests) {
        campaignService.bindQuizzesToRound(id, requests);
        return ResponseEntity.ok(ResponseDto.success(null, "Cập nhật danh sách quiz cho round thành công"));
    }
}

