package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateSchoolCampaignRequest {

    @NotBlank
    private String campaignName;

    private String description;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    private LocalDateTime invitationDate;

    private LocalDateTime invitationDeadline;

    private Integer topRankingCount;

    private String bannerImageUrl;
}
