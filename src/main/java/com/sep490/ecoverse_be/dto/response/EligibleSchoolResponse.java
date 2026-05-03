package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record EligibleSchoolResponse(
        UUID schoolId,
        String schoolName,
        String ward,
        String province,
        long managedStudentCount
) {}
