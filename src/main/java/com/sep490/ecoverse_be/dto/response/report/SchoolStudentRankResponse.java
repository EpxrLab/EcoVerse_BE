package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchoolStudentRankResponse {

    private UUID studentId;
    private String fullName;
    private String className;
    private String gradeLevel;
    private BigDecimal totalCoins;
    private Double avgGameAccuracy;
    private Double avgQuizScore;
    private long totalCampaignsJoined;
    private long totalTitlesEarned;
}
