package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record StudentQuestionResponse(
        UUID questionId,
        int questionOrder,
        String questionText,
        String questionImageUrl,
        // Danh sach dap an - KHONG co isCorrect de tranh gian lan
        List<StudentAnswerOptionResponse> answers
) {}
