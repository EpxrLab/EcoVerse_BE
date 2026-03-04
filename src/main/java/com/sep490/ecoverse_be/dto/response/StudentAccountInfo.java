package com.sep490.ecoverse_be.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentAccountInfo {

    private String studentFullName;
    private String studentCode;
    private String password;
    private String className;
    private String gradeLevel;
}
