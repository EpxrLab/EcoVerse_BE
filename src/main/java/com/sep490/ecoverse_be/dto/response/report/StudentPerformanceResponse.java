package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentPerformanceResponse {

    // Game stats
    private long totalGameSessionsCompleted;
    private Double avgGameAccuracy;
    private BigDecimal bestGameAccuracy;
    private long totalGameSessionsInPeriod;
    private Double avgGameAccuracyInPeriod;

    // Quiz stats
    private long totalQuizAttemptsCompleted;
    private Double avgQuizScore;
    private Double quizPassRate;
    private long totalQuizAttemptsInPeriod;
    private Double avgQuizScoreInPeriod;

    // Per-campaign breakdown
    private List<CampaignPerformanceDto> campaignBreakdown;

    private String period;
    private String fromDate;
    private String toDate;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CampaignPerformanceDto {
        private String campaignCode;
        private String campaignName;
        private String campaignType;
        private String campaignStatus;
        private Integer overallRank;
        private Integer schoolRank;
        private BigDecimal combinedAccuracy;
        private long gamesCompleted;
        private long quizzesCompleted;
        private Integer totalCoinsEarned;
    }
}
