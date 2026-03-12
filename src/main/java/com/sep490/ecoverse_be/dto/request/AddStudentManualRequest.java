package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddStudentManualRequest {

    @NotBlank(message = "Ten hoc sinh khong duoc rong")
    private String studentFullName;

    @NotBlank(message = "Ten lop khong duoc rong")
    private String className;

    @NotBlank(message = "Khoi lop khong duoc rong")
    private String gradeLevel;

    @NotNull(message = "Ngay sinh khong duoc rong")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gioi tinh khong duoc rong")
    @Pattern(regexp = "^(MALE|FEMALE)$", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Gioi tinh phai la MALE hoac FEMALE")
    private String gender;

    @NotBlank(message = "Dia chi khong duoc rong")
    private String address;

    @NotBlank(message = "Ten phu huynh khong duoc rong")
    private String parentFullName;

    @NotBlank(message = "So dien thoai phu huynh khong duoc rong")
    @Pattern(regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "So dien thoai phai co 10 chu so, bat dau bang 03/05/07/08/09")
    private String parentPhone;

    @NotBlank(message = "Email phu huynh khong duoc rong")
    @Email(message = "Email khong hop le")
    private String parentEmail;
}
