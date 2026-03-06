package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.ParentProfileResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipProfileResponse;
import com.sep490.ecoverse_be.dto.response.SchoolProfileResponse;
import com.sep490.ecoverse_be.dto.response.StudentProfileResponse;
import com.sep490.ecoverse_be.dto.response.UserMeResponse;
import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Student;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.repository.ParentRepository;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileServiceImpl implements IProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Override
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
    }

    @Override
    public StudentProfileResponse getStudentProfile(UUID userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));

        return StudentProfileResponse.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .className(student.getClassName())
                .gradeLevel(student.getGradeLevel())
                .academicYear(student.getAcademicYear() != null ? student.getAcademicYear().getName() : null)
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender() != null ? student.getGender().name() : null)
                .address(student.getAddress())
                .avatarUrl(student.getAvatarUrl())
                .totalCoins(student.getTotalCoins() != null ? student.getTotalCoins().toPlainString() : "0.00")
                .isFirstLogin(student.getIsFirstLogin())
                .school(StudentProfileResponse.SchoolSummary.builder()
                        .id(student.getSchool().getId())
                        .schoolName(student.getSchool().getSchoolName())
                        .build())
                .build();
    }

    @Override
    public ParentProfileResponse getParentProfile(UUID userId) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phụ huynh"));

        List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
        List<ParentProfileResponse.ChildSummary> children = links.stream()
                .map(link -> {
                    Student s = link.getStudent();
                    return ParentProfileResponse.ChildSummary.builder()
                            .id(s.getId())
                            .studentCode(s.getStudentCode())
                            .fullName(s.getFullName())
                            .className(s.getClassName())
                            .gradeLevel(s.getGradeLevel())
                            .schoolName(s.getSchool().getSchoolName())
                            .build();
                })
                .toList();

        return ParentProfileResponse.builder()
                .id(parent.getId())
                .fullName(parent.getFullName())
                .phoneNumber(parent.getPhoneNumber())
                .email(parent.getUser().getEmail())
                .isFirstLogin(parent.getIsFirstLogin())
                .children(children)
                .build();
    }

    @Override
    public SchoolProfileResponse getSchoolProfile(UUID userId) {
        School school = schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));

        return SchoolProfileResponse.builder()
                .id(school.getId())
                .schoolName(school.getSchoolName())
                .schoolType(school.getSchoolType() != null ? school.getSchoolType().name() : null)
                .taxCode(school.getTaxCode())
                .address(school.getAddress())
                .district(school.getDistrict())
                .province(school.getProvince())
                .phoneNumber(school.getPhoneNumber())
                .principalName(school.getPrincipalName())
                .position(school.getPosition())
                .contactEmail(school.getContactEmail())
                .linkWeb(school.getLinkWeb())
                .description(school.getDescription())
                .approvalStatus(school.getApprovalStatus() != null ? school.getApprovalStatus().name() : null)
                .logoUrl(school.getLogoUrl())
                .licenseUrl(school.getLicenseUrl())
                .build();
    }

    @Override
    public PartnershipProfileResponse getPartnershipProfile(UUID userId) {
        Partnership partnership = partnershipRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đối tác"));

        return PartnershipProfileResponse.builder()
                .id(partnership.getId())
                .organizationName(partnership.getOrganizationName())
                .partnershipType(partnership.getPartnershipType() != null ? partnership.getPartnershipType().name() : null)
                .contactEmail(partnership.getContactEmail())
                .phoneNumber(partnership.getPhoneNumber())
                .registeredAddress(partnership.getRegisteredAddress())
                .geographicScopeDistrict(partnership.getGeographicScopeDistrict())
                .geographicScopeProvince(partnership.getGeographicScopeProvince())
                .contactPerson(partnership.getContactPerson())
                .position(partnership.getPosition())
                .taxCode(partnership.getTaxCode())
                .linkWeb(partnership.getLinkWeb())
                .description(partnership.getDescription())
                .approvalStatus(partnership.getApprovalStatus() != null ? partnership.getApprovalStatus().name() : null)
                .logoUrl(partnership.getLogoUrl())
                .licenseUrl(partnership.getLicenseUrl())
                .build();
    }
}
