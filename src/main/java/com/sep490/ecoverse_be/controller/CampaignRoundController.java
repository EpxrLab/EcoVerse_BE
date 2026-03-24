package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.BindRoundQuizRequest;
import com.sep490.ecoverse_be.dto.request.UpdateRoundGameConfigRequest;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.ICampaignService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/quizzes/bind-existing")
    public ResponseEntity<ResponseDto<Void>> bindQuiz(@PathVariable UUID id,
                                                      @Valid @RequestBody BindRoundQuizRequest request) {
        campaignService.bindExistingQuiz(id, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Gắn quiz cho round thành công"));
    }

    @PutMapping("/{id}/quizzes/bind-existing")
    public ResponseEntity<ResponseDto<Void>> rebindQuiz(@PathVariable UUID id,
                                                        @Valid @RequestBody BindRoundQuizRequest request) {
        campaignService.bindExistingQuiz(id, request);
        return ResponseEntity.ok(ResponseDto.success(null, "Cập nhật quiz cho round thành công"));
    }
}

