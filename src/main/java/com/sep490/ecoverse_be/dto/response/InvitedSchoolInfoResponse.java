package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record InvitedSchoolInfoResponse(
        UUID invitationId,
        UUID schoolId,
        String schoolName,
        ParticipationStatus status,
        int studentsEnrolled,
        LocalDateTime invitationSentAt,
        LocalDateTime participationConfirmedAt
) {}
