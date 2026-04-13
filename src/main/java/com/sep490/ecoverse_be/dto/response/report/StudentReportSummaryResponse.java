package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentReportSummaryResponse {

    private String fullName;
    private String className;
    private String gradeLevel;

    // Coin
    private BigDecimal currentCoinBalance;
    private BigDecimal totalCoinsEarned;
    private BigDecimal totalCoinsSpent;

    // Campaign
    private long totalCampaignsJoined;
    private long activeCampaigns;

    // Game
    private long totalGameSessionsCompleted;
    private Double avgGameAccuracy;
    private BigDecimal bestGameAccuracy;

    // Quiz
    private long totalQuizAttemptsCompleted;
    private Double avgQuizScore;
    private Double quizPassRate;

    // Ranking
    private Integer bestOverallRank;

    // Achievement
    private long totalTitlesEarned;

    // Reward
    private long totalRewardRequestsMade;
    private long pendingRewardRequests;

    private String period;
    private String fromDate;
    private String toDate;
}
