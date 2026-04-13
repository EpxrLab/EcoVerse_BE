package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.Gender;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentAccountInfo {

    private UUID studentId;
    private String studentFullName;
    private String studentCode;
    private String password;
    private String className;
    private String gradeLevel;
    private String address;
    private LocalDate dob;
    private Gender gender;
    private Boolean active;
    private BigDecimal totalCoin;
    private AccountStatus accountStatus;
}
