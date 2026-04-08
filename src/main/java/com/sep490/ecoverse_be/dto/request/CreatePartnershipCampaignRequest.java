package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreatePartnershipCampaignRequest {

    @NotBlank
    private String campaignName;

    private String description;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    private LocalDateTime registrationDate;

    private LocalDateTime registrationDeadline;

    private LocalDateTime invitationDate;

    private LocalDateTime invitationDeadline;

    private Integer maxStudentsPerSchool;

    private Integer totalStudentQuota;

    private Integer topRankingCount;

    private String bannerImageUrl;

    private List<UUID> schoolIds;

    @Valid
    private List<CampaignRewardRequest> rewards;

    @NotEmpty
    @Valid
    private List<PartnershipRoundRequest> rounds;
}

