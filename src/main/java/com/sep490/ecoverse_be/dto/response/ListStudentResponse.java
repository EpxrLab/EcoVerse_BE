package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListStudentResponse {
    private UUID studentId;
    private String studentFullName;
    private String studentCode;
    private String className;
    private String gradeLevel;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String avatarUrl;
    private String avatarPresignedUrl;
}
