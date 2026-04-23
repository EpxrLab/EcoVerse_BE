package com.sep490.ecoverse_be.dto.response.report;

import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchoolReportSummaryResponse {

    // Students
    private long totalStudents;

    // Campaigns
    private long totalCampaignsCreated;
    private long activeCampaignsCreated;
    private long completedCampaignsCreated;
    private long totalCampaignsParticipated;

    // Reward management
    private long pendingRewardRequests;
    private long totalRewardRequestsProcessed;

    // Subscription
    private SubscriptionStatus subscriptionStatus;
    private OffsetDateTime subscriptionEndDate;
    private String subscriptionPlanName;

    // Top performers
    private List<SchoolStudentRankResponse> topStudentsByCoins;
    private List<SchoolStudentRankResponse> topStudentsByAccuracy;

    private String period;
    private String fromDate;
    private String toDate;
}
