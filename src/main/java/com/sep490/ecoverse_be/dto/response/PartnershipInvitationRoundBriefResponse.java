package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PartnershipInvitationRoundBriefResponse(
        Integer roundNumber,
        UUID roundId,
        String roundName,
        Integer maxParticipants,
        Integer advanceCount,
        boolean isFinalRound
) {
}
