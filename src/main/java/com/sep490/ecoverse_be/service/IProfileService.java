package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.ParentProfileResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipProfileResponse;
import com.sep490.ecoverse_be.dto.response.SchoolProfileResponse;
import com.sep490.ecoverse_be.dto.response.StudentProfileResponse;
import com.sep490.ecoverse_be.dto.response.UserMeResponse;

import java.util.UUID;

public interface IProfileService {

    UserMeResponse getMe(UUID userId);

    StudentProfileResponse getStudentProfile(UUID userId);

    ParentProfileResponse getParentProfile(UUID userId);

    SchoolProfileResponse getSchoolProfile(UUID userId);

    PartnershipProfileResponse getPartnershipProfile(UUID userId);
}
