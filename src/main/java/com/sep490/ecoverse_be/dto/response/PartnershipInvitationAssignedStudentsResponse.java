package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record PartnershipInvitationAssignedStudentsResponse(
        UUID invitationId,
        UUID campaignId,
        String campaignName,
        Integer maxStudentsPerSchool,
        int selectedCount,
        Integer remainingSlots,
        List<PartnershipInvitationAssignedStudentResponse> selectedStudents
) {
}
