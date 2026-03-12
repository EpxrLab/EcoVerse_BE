package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.UpdatePartnershipProfileRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSchoolProfileRequest;
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
import org.springframework.transaction.annotation.Transactional;

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
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender() != null ? student.getGender().name() : null)
                .address(student.getAddress())
                .avatarUrl(student.getAvatarUrl())
                .totalCoins(student.getTotalCoins() != null ? student.getTotalCoins() : null)
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
                .ward(school.getWard())
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
                .geographicScopeWard(partnership.getGeographicScopeWard())
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

    @Override
    @Transactional
    public SchoolProfileResponse updateSchoolProfile(UUID userId, UpdateSchoolProfileRequest request) {
        School school = schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));

        // Chỉ cập nhật khi giá trị không null để hỗ trợ partial update
        if (request.getSchoolName() != null) school.setSchoolName(request.getSchoolName());
        if (request.getSchoolType() != null) school.setSchoolType(request.getSchoolType());
        if (request.getAddress() != null) school.setAddress(request.getAddress());
        if (request.getWard() != null) school.setWard(request.getWard());
        if (request.getProvince() != null) school.setProvince(request.getProvince());
        if (request.getPhoneNumber() != null) school.setPhoneNumber(request.getPhoneNumber());
        if (request.getPrincipalName() != null) school.setPrincipalName(request.getPrincipalName());
        if (request.getPosition() != null) school.setPosition(request.getPosition());
        if (request.getContactEmail() != null) school.setContactEmail(request.getContactEmail());
        if (request.getLinkWeb() != null) school.setLinkWeb(request.getLinkWeb());
        if (request.getDescription() != null) school.setDescription(request.getDescription());

        schoolRepository.save(school);

        return SchoolProfileResponse.builder()
                .id(school.getId())
                .schoolName(school.getSchoolName())
                .schoolType(school.getSchoolType() != null ? school.getSchoolType().name() : null)
                .taxCode(school.getTaxCode())
                .address(school.getAddress())
                .ward(school.getWard())
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
    @Transactional
    public PartnershipProfileResponse updatePartnershipProfile(UUID userId, UpdatePartnershipProfileRequest request) {
        Partnership partnership = partnershipRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đối tác"));

        // Chỉ cập nhật khi giá trị không null để hỗ trợ partial update
        if (request.getOrganizationName() != null) partnership.setOrganizationName(request.getOrganizationName());
        if (request.getPartnershipType() != null) partnership.setPartnershipType(request.getPartnershipType());
        if (request.getContactEmail() != null) partnership.setContactEmail(request.getContactEmail());
        if (request.getPhoneNumber() != null) partnership.setPhoneNumber(request.getPhoneNumber());
        if (request.getRegisteredAddress() != null) partnership.setRegisteredAddress(request.getRegisteredAddress());
        if (request.getGeographicScopeWard() != null) partnership.setGeographicScopeWard(request.getGeographicScopeWard());
        if (request.getGeographicScopeProvince() != null) partnership.setGeographicScopeProvince(request.getGeographicScopeProvince());
        if (request.getContactPerson() != null) partnership.setContactPerson(request.getContactPerson());
        if (request.getPosition() != null) partnership.setPosition(request.getPosition());
        if (request.getLinkWeb() != null) partnership.setLinkWeb(request.getLinkWeb());
        if (request.getDescription() != null) partnership.setDescription(request.getDescription());

        partnershipRepository.save(partnership);

        return PartnershipProfileResponse.builder()
                .id(partnership.getId())
                .organizationName(partnership.getOrganizationName())
                .partnershipType(partnership.getPartnershipType() != null ? partnership.getPartnershipType().name() : null)
                .contactEmail(partnership.getContactEmail())
                .phoneNumber(partnership.getPhoneNumber())
                .registeredAddress(partnership.getRegisteredAddress())
                .geographicScopeWard(partnership.getGeographicScopeWard())
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
