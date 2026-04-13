package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ParentCampaignApprovalRequest {

    @NotNull
    private UUID studentId;

    private String reason;
}

