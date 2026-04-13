package com.sep490.ecoverse_be.dto.response.report;

import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnershipCampaignReportResponse {

    private UUID campaignId;
    private String campaignCode;
    private String campaignName;
    private PartnershipCampaignStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int totalRounds;

    private long schoolsParticipated;
    private long totalStudentsEnrolled;
    private Double avgParticipantAccuracy;
}
