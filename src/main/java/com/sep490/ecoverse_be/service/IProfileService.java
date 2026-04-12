package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.UpdatePartnershipProfileRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSchoolProfileRequest;
import com.sep490.ecoverse_be.dto.response.ParentProfileResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipProfileResponse;
import com.sep490.ecoverse_be.dto.response.SchoolProfileResponse;
import com.sep490.ecoverse_be.dto.response.StudentProfileResponse;
import com.sep490.ecoverse_be.dto.response.UserMeResponse;
import com.sep490.ecoverse_be.entity.Student;

import java.util.UUID;

public interface IProfileService {

    UserMeResponse getMe(UUID userId);

    StudentProfileResponse getStudentProfile(UUID userId);

    /** Dựng hồ sơ học sinh đầy đủ (kèm danh hiệu), dùng chung cho GET profile và sau cập nhật hồ sơ. */
    StudentProfileResponse buildStudentProfileResponse(Student student);

    ParentProfileResponse getParentProfile(UUID userId);

    SchoolProfileResponse getSchoolProfile(UUID userId);

    PartnershipProfileResponse getPartnershipProfile(UUID userId);

    SchoolProfileResponse updateSchoolProfile(UUID userId, UpdateSchoolProfileRequest request);

    PartnershipProfileResponse updatePartnershipProfile(UUID userId, UpdatePartnershipProfileRequest request);
}
