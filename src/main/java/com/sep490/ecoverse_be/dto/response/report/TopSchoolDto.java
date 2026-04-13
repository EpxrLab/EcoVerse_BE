package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopSchoolDto {

    private UUID schoolId;
    private String schoolName;
    private long totalStudentsEnrolled;
    private long campaignsParticipated;
}
