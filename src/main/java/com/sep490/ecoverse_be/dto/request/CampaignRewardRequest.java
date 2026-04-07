package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignRewardRequest {

    @NotNull
    @Min(1)
    private Integer rankPosition;

    @NotBlank
    private String rewardName;

    private String description;

    private String imageUrl;

    private String sponsorName;
}
