package com.sep490.ecoverse_be.dto.response.report;

import com.sep490.ecoverse_be.enums.CampaignType;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchoolCampaignReportResponse {

    private UUID campaignId;
    private String campaignCode;
    private String campaignName;
    private CampaignType campaignType;
    private String status;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    // Participation stats
    private int studentsEnrolled;
    private long studentsCompleted;

    // Performance
    private Double avgCombinedAccuracy;

    private boolean isCreator;
}
