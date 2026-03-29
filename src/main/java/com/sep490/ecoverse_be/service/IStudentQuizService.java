package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.SubmitQuizAttemptRequest;
import com.sep490.ecoverse_be.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface IStudentQuizService {

    // Bat dau lam quiz: validate rules, tao attempt, tra ve cau hoi (khong co dap an dung)
    StudentQuizDetailResponse startQuizAttempt(UUID campaignId, UUID roundId, UUID quizId);

    // Nop bai: tinh diem, luu ket qua, cap nhat leaderboard
    QuizAttemptResultResponse submitQuizAttempt(UUID attemptId, SubmitQuizAttemptRequest request);

    // Xem tien do lam quiz cua round: danh sach quiz + status moi quiz
    StudentRoundQuizProgressResponse getRoundQuizProgress(UUID campaignId, UUID roundId);

    // Xem lai ket qua chi tiet cua 1 lan lam
    QuizAttemptResultResponse getQuizAttemptResult(UUID attemptId);

    // Lich su tat ca lan lam 1 quiz trong round
    List<QuizAttemptSummaryResponse> getQuizAttemptHistory(UUID campaignId, UUID roundId, UUID quizId);
}
