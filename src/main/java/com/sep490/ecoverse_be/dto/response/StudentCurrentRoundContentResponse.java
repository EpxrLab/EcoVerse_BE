package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record StudentCurrentRoundContentResponse(
        UUID campaignId,
        UUID roundId,
        Integer roundNumber,
        String roundName,
        LocalDateTime roundStartTime,
        LocalDateTime roundEndTime,
        Long secondsToNextRound,
        Long secondsToCampaignEnd,
        List<StudentRoundGameConfigResponse> games,
        List<RoundQuizBriefResponse> quizzes
) {
}
