package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PartnershipCampaignSchoolRequest {

    @NotNull
    private UUID schoolId;

    /** Số học sinh tối đa được mời cho trường này trong chiến dịch. */
    @NotNull
    @Positive
    private Integer maxStudentsInvited;
}
