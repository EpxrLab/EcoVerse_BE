package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record StudentRoundHistoryResponse(
        List<GameHistoryGroupResponse> gameHistories,
        List<QuizHistoryGroupResponse> quizHistories
) {}
