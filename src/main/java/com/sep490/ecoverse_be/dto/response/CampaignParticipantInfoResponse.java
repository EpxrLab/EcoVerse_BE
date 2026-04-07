package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CampaignParticipantInfoResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String gradeLevel,
        String className,
        ParticipationStatus parentApprovalStatus,
        LocalDateTime invitationSentAt,
        String rejectionReason
) {
}
