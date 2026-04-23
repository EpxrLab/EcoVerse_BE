package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private String address;
    private String avatarUrl;
    private String avatarPresignedUrl;
    private BigDecimal totalCoins;
    private Boolean isFirstLogin;
    private SchoolSummary school;

    /** Danh hiệu đạt được trong các chiến dịch (mới nhất trước) */
    private List<EarnedTitleItem> earnedTitles;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SchoolSummary {
        private UUID id;
        private String schoolName;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EarnedTitleItem {
        private UUID studentTitleId;
        private UUID campaignId;
        private String campaignName;
        private UUID campaignTitleId;
        private String titleName;
        private String criteriaType;
        private String displayText;
        private String metricValue;
        private OffsetDateTime earnedAt;
        private Boolean isDisplayed;
    }
}
