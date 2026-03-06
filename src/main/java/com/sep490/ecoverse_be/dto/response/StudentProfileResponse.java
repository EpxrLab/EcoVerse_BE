package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileResponse {

    private UUID id;
    private String studentCode;
    private String fullName;
    private String className;
    private String gradeLevel;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatarUrl;
    private String totalCoins;
    private SchoolSummary school;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SchoolSummary {
        private UUID id;
        private String schoolName;
    }
}
