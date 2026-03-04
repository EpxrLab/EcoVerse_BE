package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.PartnershipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyRegisterPartnershipRequest {

    @NotBlank(message = "Organization name is required")
    private  String organizationName;


    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format.")
    private String contactEmail;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Phone number must be 10 digits and start with 03, 05, 07, 08, or 09"
    )
    private String phoneNumber;

    @NotBlank(message = "Province/City is required")
    private String province;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "Contact person name is required")
    private String contactPerson;

    @NotBlank(message = "Position is required")
    private String position;

    @NotBlank(message = "Tax code is required")
    private String taxCode;

    private String linkWeb;

    private String description;

    @NotBlank(message = "Partnership type is required")
    private PartnershipType partnershipType;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must have at least 8 characters.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Passwords must include uppercase letters, lowercase letters, numbers, and special characters.")
    private String password;

    private String otp;

    @NotBlank(message = "Logo URL is required")
    private String logoUrl;

    @NotBlank(message = "License URL is required")
    private String licenseUrl;
}
