package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.entity.AcademicYear;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentInformationRequest {
    private String fullName;
    private String studentCode;
    private String className;
    private String gradeLevel;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
}
