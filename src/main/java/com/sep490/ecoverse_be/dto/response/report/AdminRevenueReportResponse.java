package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminRevenueReportResponse {

    private BigDecimal totalRevenueAllTime;
    private BigDecimal totalRevenueInPeriod;
    private BigDecimal revenueFromSchools;
    private BigDecimal revenueFromPartnerships;

    private long totalSuccessfulPayments;
    private long activeSubscriptions;
    private long activeSchoolSubscriptions;
    private long activePartnershipSubscriptions;

    private List<MonthlyRevenueTrendDto> monthlyTrend;

    private String period;
    private String fromDate;
    private String toDate;
}
