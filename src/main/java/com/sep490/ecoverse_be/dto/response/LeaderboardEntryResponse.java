package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record LeaderboardEntryResponse(
        UUID studentId,
        String studentName,
        UUID schoolId,
        String schoolName,
        BigDecimal combinedAccuracyPercentage,
        BigDecimal avgTimeSeconds,
        Integer rank,
        Integer totalCoinsEarned
) {
}

