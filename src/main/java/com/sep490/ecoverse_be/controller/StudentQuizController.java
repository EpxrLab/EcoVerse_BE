package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.SubmitQuizAttemptRequest;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.service.IStudentQuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/quiz")
@PreAuthorize("hasAuthority('STUDENT')")
public class StudentQuizController {

    @Autowired
    private IStudentQuizService studentQuizService;

    // Bat dau lam quiz: tra ve cau hoi (khong co dap an dung)
    @PostMapping("/campaigns/{campaignId}/rounds/{roundId}/quizzes/{quizId}/start")
    public ResponseEntity<ResponseDto<StudentQuizDetailResponse>> startQuizAttempt(
            @PathVariable UUID campaignId,
            @PathVariable UUID roundId,
            @PathVariable UUID quizId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentQuizService.startQuizAttempt(campaignId, roundId, quizId),
                "Bắt đầu làm quiz thành công"));
    }

    // Nop bai: tinh diem, luu ket qua, cap nhat leaderboard
    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public ResponseEntity<ResponseDto<QuizAttemptResultResponse>> submitQuizAttempt(
            @PathVariable UUID attemptId,
            @Valid @RequestBody SubmitQuizAttemptRequest request) {
        return ResponseEntity.ok(ResponseDto.success(
                studentQuizService.submitQuizAttempt(attemptId, request),
                "Nộp bài thành công"));
    }

    // Xem tien do quiz trong round
    @GetMapping("/campaigns/{campaignId}/rounds/{roundId}/quiz-progress")
    public ResponseEntity<ResponseDto<StudentRoundQuizProgressResponse>> getRoundQuizProgress(
            @PathVariable UUID campaignId,
            @PathVariable UUID roundId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentQuizService.getRoundQuizProgress(campaignId, roundId),
                "Lấy tiến độ quiz thành công"));
    }

    // Xem lai ket qua chi tiet cua 1 lan lam
    @GetMapping("/quiz-attempts/{attemptId}/result")
    public ResponseEntity<ResponseDto<QuizAttemptResultResponse>> getQuizAttemptResult(
            @PathVariable UUID attemptId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentQuizService.getQuizAttemptResult(attemptId),
                "Lấy kết quả bài làm thành công"));
    }

    // Lich su tat ca lan lam 1 quiz trong round
    @GetMapping("/campaigns/{campaignId}/rounds/{roundId}/quizzes/{quizId}/history")
    public ResponseEntity<ResponseDto<List<QuizAttemptSummaryResponse>>> getQuizAttemptHistory(
            @PathVariable UUID campaignId,
            @PathVariable UUID roundId,
            @PathVariable UUID quizId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentQuizService.getQuizAttemptHistory(campaignId, roundId, quizId),
                "Lấy lịch sử làm quiz thành công"));
    }
}
