package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
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
    private String academicYear;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String avatarUrl;
    private BigDecimal totalCoins;
    private Boolean isFirstLogin;
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
