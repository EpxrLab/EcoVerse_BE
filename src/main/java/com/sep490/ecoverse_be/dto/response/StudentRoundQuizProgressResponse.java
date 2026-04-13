package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record StudentRoundQuizProgressResponse(
        UUID roundId,
        String roundName,
        int totalQuizzes,
        int completedQuizzes,
        // Student da hoan thanh het tat ca quiz required chua -> du dieu kien xep hang
        boolean isEligibleForRanking,
        List<QuizProgressItem> quizzes
) {}
