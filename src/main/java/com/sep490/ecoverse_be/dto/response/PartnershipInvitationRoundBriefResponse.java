package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

@Builder
public record PartnershipInvitationRoundBriefResponse(
        Integer roundNumber,
        String roundName,
        Integer maxParticipants,
        Integer advanceCount,
        boolean isFinalRound
) {
}
