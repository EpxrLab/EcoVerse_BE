package com.sep490.ecoverse_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentExcelRowDto {

    private int rowNumber;
    private String studentFullName;
    private String className;
    private String gradeLevel;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String parentFullName;
    private String parentPhone;
    private String parentEmail;
}
