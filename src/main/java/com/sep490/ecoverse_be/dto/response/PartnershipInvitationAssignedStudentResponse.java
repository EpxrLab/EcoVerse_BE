package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public record PartnershipInvitationAssignedStudentResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String gradeLevel,
        String className,
        ParticipationStatus parentApprovalStatus,
        OffsetDateTime invitationSentAt
) {
}
