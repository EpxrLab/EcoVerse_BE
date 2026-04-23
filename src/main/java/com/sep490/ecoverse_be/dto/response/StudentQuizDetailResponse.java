package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Builder
public record StudentQuizDetailResponse(
        UUID quizId,
        String title,
        String description,
        Integer timePerQuestion,
        int passScorePercentage,
        int questionCount,
        // Thong tin lan lam hien tai
        UUID attemptId,
        int attemptNumber,
        int maxAttempts,
        OffsetDateTime startTime,
        // Danh sach cau hoi (dap an da duoc xao tron, khong co isCorrect)
        List<StudentQuestionResponse> questions
) {}
