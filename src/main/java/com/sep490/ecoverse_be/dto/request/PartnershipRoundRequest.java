package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PartnershipRoundRequest {

    @NotNull
    private Integer roundNumber;

    @NotBlank
    private String roundName;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private Integer maxParticipants;

    private Integer advanceCount;

    private Boolean isFinalRound;
}

