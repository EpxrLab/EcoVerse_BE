package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParentProfileResponse {

    private UUID id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private Boolean isFirstLogin;
    private List<ChildSummary> children;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChildSummary {
        private UUID id;
        private String studentCode;
        private String fullName;
        private String className;
        private String gradeLevel;
        private String schoolName;
    }
}
