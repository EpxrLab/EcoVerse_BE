package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public record InvitedSchoolInfoResponse(
        UUID invitationId,
        UUID schoolId,
        String schoolName,
        ParticipationStatus status,
        int studentsEnrolled,
        OffsetDateTime invitationSentAt,
        OffsetDateTime participationConfirmedAt
) {}
