package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.SubmitGameSessionRequest;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionResultResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionStartResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionSummaryResponse;
import com.sep490.ecoverse_be.service.IStudentGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/game")
@PreAuthorize("hasAuthority('STUDENT')")
@RequiredArgsConstructor
public class StudentGameController {

    private final IStudentGameService studentGameService;

    @PostMapping("/campaigns/{campaignId}/rounds/{roundId}/configs/{roundGameConfigId}/start")
    public ResponseEntity<ResponseDto<StudentGameSessionStartResponse>> startGameSession(
            @PathVariable UUID campaignId,
            @PathVariable UUID roundId,
                        @PathVariable UUID roundGameConfigId,
                        @RequestParam(required = false, defaultValue = "1") Integer levelNumber) {
        return ResponseEntity.ok(ResponseDto.success(
                                studentGameService.startGameSession(campaignId, roundId, roundGameConfigId, levelNumber),
                "Bắt đầu phiên chơi game thành công"
        ));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public ResponseEntity<ResponseDto<StudentGameSessionResultResponse>> submitGameSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitGameSessionRequest request) {
        return ResponseEntity.ok(ResponseDto.success(
                studentGameService.submitGameSession(sessionId, request),
                "Nộp kết quả phiên chơi thành công"
        ));
    }

    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<ResponseDto<StudentGameSessionResultResponse>> getGameSessionResult(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentGameService.getGameSessionResult(sessionId),
                "Lấy kết quả phiên chơi thành công"
        ));
    }

    @GetMapping("/campaigns/{campaignId}/rounds/{roundId}/configs/{roundGameConfigId}/history")
    public ResponseEntity<ResponseDto<List<StudentGameSessionSummaryResponse>>> getGameSessionHistory(
            @PathVariable UUID campaignId,
            @PathVariable UUID roundId,
            @PathVariable UUID roundGameConfigId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentGameService.getGameSessionHistory(campaignId, roundId, roundGameConfigId),
                "Lấy lịch sử phiên chơi thành công"
        ));
    }
}
