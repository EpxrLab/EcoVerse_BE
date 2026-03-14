package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.ParentAdminDetail;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.dto.response.StudentAdminDetail;
import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Student;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.ParentRepository;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IAdminService;
import com.sep490.ecoverse_be.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements IAdminService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IEmailService emailService;

    private User getCurrentAdmin() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUser();
    }

    @Override
    public List<SchoolDetailResponse> getPendingSchools() {
        return schoolRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::mapToSchoolDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnershipDetailResponse> getPendingPartnerships() {
        return partnershipRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::mapToPartnershipDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SchoolDetailResponse updateSchoolApproval(
            UUID id,
            UpdateApprovalRequest request
    ) {

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với id: " + id));

        if (school.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Trường học không ở trạng thái chờ duyệt.");
        }

        if (request.getStatus() == ApprovalStatus.REJECTED &&
                (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("Phải cung cấp lý do khi từ chối.");
        }

        school.setApprovalStatus(request.getStatus());
        school.setApprovedBy(getCurrentAdmin());
        school.setApprovedAt(LocalDateTime.now());

        if (request.getStatus() == ApprovalStatus.APPROVED) {

            User schoolUser = school.getUser();
            schoolUser.setStatus(AccountStatus.ACTIVE);

            emailService.sendApprovalEmail(
                    school.getContactEmail(),
                    school.getSchoolName()
            );

        } else {

            emailService.sendRejectionEmail(
                    school.getContactEmail(),
                    school.getSchoolName(),
                    request.getReason()
            );
        }

        return mapToSchoolDetailResponse(school);
    }

    @Override
    @Transactional
    public PartnershipDetailResponse updatePartnershipApproval(
            UUID id,
            UpdateApprovalRequest request
    ) {

        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác với id: " + id));

        if (partnership.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Đối tác không ở trạng thái chờ duyệt.");
        }

        if (request.getStatus() == ApprovalStatus.REJECTED &&
                (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("Phải cung cấp lý do khi từ chối.");
        }

        partnership.setApprovalStatus(request.getStatus());
        partnership.setApprovedBy(getCurrentAdmin());
        partnership.setApprovedAt(LocalDateTime.now());

        if (request.getStatus() == ApprovalStatus.APPROVED) {

            User schoolUser = partnership.getUser();
            schoolUser.setStatus(AccountStatus.ACTIVE);

            emailService.sendApprovalEmail(
                    partnership.getContactEmail(),
                    partnership.getOrganizationName()
            );

        } else {

            emailService.sendRejectionEmail(
                    partnership.getContactEmail(),
                    partnership.getOrganizationName(),
                    request.getReason()
            );
        }

        return mapToPartnershipDetailResponse(partnership);
    }

    @Override
    public List<SchoolDetailResponse> getApprovedSchools() {
        return schoolRepository.findByApprovalStatus(ApprovalStatus.APPROVED)
                .stream()
                .map(this::mapToSchoolDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnershipDetailResponse> getApprovedPartnerships() {
        return partnershipRepository.findByApprovalStatus(ApprovalStatus.APPROVED)
                .stream()
                .map(this::mapToPartnershipDetailResponse)
                .collect(Collectors.toList());
    }


    @Override
    public void updateUserStatus(UUID userId, boolean isActive) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if(isActive){
            user.setStatus(AccountStatus.ACTIVE);
            user.setIsActive(true);
        }else{
            user.setStatus(AccountStatus.SUSPENDED);
            user.setIsActive(false);
        }
        userRepository.save(user);
    }


    private SchoolDetailResponse mapToSchoolDetailResponse(School school) {
        User user = school.getUser();
        return SchoolDetailResponse.builder()
                .id(school.getId())
                .userId(user.getId().toString())
                .schoolName(school.getSchoolName())
                .schoolType(school.getSchoolType())
                .taxCode(school.getTaxCode())
                .contactEmail(school.getContactEmail())
                .phoneNumber(school.getPhoneNumber())
                .address(school.getAddress())
                .ward(school.getWard())
                .province(school.getProvince())
                .country(school.getCountry())
                .principalName(school.getPrincipalName())
                .position(school.getPosition())
                .linkWeb(school.getLinkWeb())
                .description(school.getDescription())
                .logoUrl(school.getLogoUrl())
                .licenseUrl(school.getLicenseUrl())
                .approvalStatus(school.getApprovalStatus())
                .approvedAt(school.getApprovedAt())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }

    private PartnershipDetailResponse mapToPartnershipDetailResponse(Partnership partnership) {
        User user = partnership.getUser();
        return PartnershipDetailResponse.builder()
                .id(partnership.getId())
                .userId(user.getId().toString())
                .organizationName(partnership.getOrganizationName())
                .partnershipType(partnership.getPartnershipType())
                .taxCode(partnership.getTaxCode())
                .contactEmail(partnership.getContactEmail())
                .phoneNumber(partnership.getPhoneNumber())
                .registeredAddress(partnership.getRegisteredAddress())
                .geographicScopeWard(partnership.getGeographicScopeWard())
                .geographicScopeProvince(partnership.getGeographicScopeProvince())
                .contactPerson(partnership.getContactPerson())
                .position(partnership.getPosition())
                .linkWeb(partnership.getLinkWeb())
                .description(partnership.getDescription())
                .logoUrl(partnership.getLogoUrl())
                .licenseUrl(partnership.getLicenseUrl())
                .approvalStatus(partnership.getApprovalStatus())
                .approvedAt(partnership.getApprovedAt())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(partnership.getCreatedAt())
                .updatedAt(partnership.getUpdatedAt())
                .build();
    }

    @Override
    public List<SchoolDetailResponse> getAllSchools() {
        return schoolRepository.findAll()
                .stream()
                .map(this::mapToSchoolDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnershipDetailResponse> getAllPartnerships() {
        return partnershipRepository.findAll()
                .stream()
                .map(this::mapToPartnershipDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminUserListResponse> getAllUsers(Role role, UUID schoolId) {
        List<AdminUserListResponse> result = new ArrayList<>();

        if (role == null || role == Role.PARTNERSHIP_SCHOOL) {
            schoolRepository.findAll()
                    .forEach(s -> result.add(mapSchoolToAdminUserResponse(s)));
        }
        if (role == null || role == Role.THIRD_PARTY_PARTNERSHIP) {
            partnershipRepository.findAll()
                    .forEach(p -> result.add(mapPartnershipToAdminUserResponse(p)));
        }

        if (role == null || role == Role.STUDENT) {
            List<Student> students = (schoolId != null)
                    ? studentRepository.findBySchoolId(schoolId)
                    : studentRepository.findAll();
            students.forEach(s -> result.add(mapStudentToAdminUserResponse(s)));
        }

        if (role == null || role == Role.PARENT) {
            List<Parent> parents;
            if (schoolId != null) {

                List<Student> students = studentRepository.findBySchoolId(schoolId);
                Map<UUID, Parent> parentMap = new LinkedHashMap<>();
                for (Student student : students) {
                    List<StudentParentLink> links = studentParentLinkRepository.findByStudentId(student.getId());
                    for (StudentParentLink link : links) {
                        parentMap.putIfAbsent(link.getParent().getId(), link.getParent());
                    }
                }
                parents = new ArrayList<>(parentMap.values());
            } else {
                parents = parentRepository.findAll();
            }
            parents.forEach(p -> result.add(mapParentToAdminUserResponse(p)));
        }

        return result;
    }

    @Override
    public AdminUserListResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với id: " + userId));

        return switch (user.getRole()) {
            case PARTNERSHIP_SCHOOL -> {
                School school = schoolRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));
                yield mapSchoolToAdminUserResponse(school);
            }
            case THIRD_PARTY_PARTNERSHIP -> {
                Partnership partnership = partnershipRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đối tác"));
                yield mapPartnershipToAdminUserResponse(partnership);
            }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));
                yield mapStudentToAdminUserResponse(student);
            }
            case PARENT -> {
                Parent parent = parentRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phụ huynh"));
                yield mapParentToAdminUserResponse(parent);
            }
            default -> AdminUserListResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .detail(null)
                    .build();
        };
    }

    @Override
    public SchoolDetailResponse getSchoolById(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với id: " + schoolId));
        return mapToSchoolDetailResponse(school);
    }

    @Override
    public PartnershipDetailResponse getPartnershipById(UUID partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác với id: " + partnershipId));
        return mapToPartnershipDetailResponse(partnership);
    }

    private AdminUserListResponse mapSchoolToAdminUserResponse(School school) {
        User user = school.getUser();
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(mapToSchoolDetailResponse(school))
                .build();
    }

    private AdminUserListResponse mapPartnershipToAdminUserResponse(Partnership partnership) {
        User user = partnership.getUser();
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(mapToPartnershipDetailResponse(partnership))
                .build();
    }

    private AdminUserListResponse mapStudentToAdminUserResponse(Student student) {
        User user = student.getUser();
        School school = student.getSchool();
        StudentAdminDetail studentDetail = StudentAdminDetail.builder()
                .studentId(student.getId())
                .fullName(student.getFullName())
                .studentCode(student.getStudentCode())
                .className(student.getClassName())
                .gradeLevel(student.getGradeLevel())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender() != null ? student.getGender().name() : null)
                .address(student.getAddress())
                .avatarUrl(student.getAvatarUrl())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .schoolName(school.getSchoolName())
                .schoolId(school.getId())
                .build();

        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(studentDetail)
                .build();
    }

    private AdminUserListResponse mapParentToAdminUserResponse(Parent parent) {
        User user = parent.getUser();
        List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
        List<String> schoolNames = links.stream()
                .map(link -> link.getStudent().getSchool().getSchoolName())
                .distinct()
                .collect(Collectors.toList());

        ParentAdminDetail parentDetail = ParentAdminDetail.builder()
                .parentId(parent.getId())
                .fullName(parent.getFullName())
                .phoneNumber(parent.getPhoneNumber())
                .email(user.getEmail())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .schoolNames(schoolNames)
                .build();

        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(parentDetail)
                .build();
    }
}
