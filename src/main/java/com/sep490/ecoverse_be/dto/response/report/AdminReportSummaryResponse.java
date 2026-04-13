package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminReportSummaryResponse {

    // Users
    private long totalStudents;
    private long totalParents;
    private long totalSchools;
    private long totalPartnerships;
    private long newRegistrationsInPeriod;

    // Approvals pending
    private long pendingSchools;
    private long pendingPartnerships;

    // Campaigns
    private long totalSchoolCampaigns;
    private long totalPartnershipCampaigns;
    private long activeCampaigns;

    // Revenue (quick overview)
    private BigDecimal totalRevenueAllTime;
    private BigDecimal totalRevenueInPeriod;
    private long activeSubscriptions;

    // Platform activity
    private long totalGameSessionsCompleted;
    private long totalQuizAttemptsCompleted;

    // Revenue trend for quick chart
    private List<MonthlyRevenueTrendDto> last12MonthsRevenueTrend;

    private String period;
    private String fromDate;
    private String toDate;
}
