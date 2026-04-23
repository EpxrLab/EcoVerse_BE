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
public class PartnershipReportSummaryResponse {

    // Campaigns
    private long totalCampaignsCreated;
    private long activeCampaigns;
    private long completedCampaigns;

    // Reach
    private long totalSchoolsParticipated;
    private long totalStudentsReached;
    private Double avgParticipantAccuracy;

    // Subscription
    private SubscriptionStatus subscriptionStatus;
    private OffsetDateTime subscriptionEndDate;
    private String subscriptionPlanName;

    // Top schools
    private List<TopSchoolDto> topSchoolsByParticipation;

    private String period;
    private String fromDate;
    private String toDate;
}
