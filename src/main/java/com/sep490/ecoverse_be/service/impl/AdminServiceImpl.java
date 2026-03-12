package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
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
        return SchoolDetailResponse.builder()
                .id(school.getId())
                .userId(school.getUser().getId().toString())
                .schoolName(school.getSchoolName())
                .schoolType(school.getSchoolType())
                .taxCode(school.getTaxCode())
                .contactEmail(school.getContactEmail())
                .phoneNumber(school.getPhoneNumber())
                .address(school.getAddress())
                .ward(school.getWard())
                .province(school.getProvince())
                .principalName(school.getPrincipalName())
                .position(school.getPosition())
                .logoUrl(school.getLogoUrl())
                .licenseUrl(school.getLicenseUrl())
                .approvalStatus(school.getApprovalStatus())
                .createdAt(school.getCreatedAt())
                .build();
    }

    private PartnershipDetailResponse mapToPartnershipDetailResponse(Partnership partnership) {
        return PartnershipDetailResponse.builder()
                .id(partnership.getId())
                .userId(partnership.getUser().getId().toString())
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
                .logoUrl(partnership.getLogoUrl())
                .licenseUrl(partnership.getLicenseUrl())
                .approvalStatus(partnership.getApprovalStatus())
                .createdAt(partnership.getCreatedAt())
                .build();
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

        // Dựa vào role để lấy entity tương ứng và map sang response
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
                .schoolId(school.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .displayName(school.getSchoolName())
                .build();
    }

    private AdminUserListResponse mapPartnershipToAdminUserResponse(Partnership partnership) {
        User user = partnership.getUser();
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .partnerId(partnership.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .displayName(partnership.getOrganizationName())
                .build();
    }

    private AdminUserListResponse mapStudentToAdminUserResponse(Student student) {
        User user = student.getUser();
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .displayName(student.getFullName())
                .schoolName(student.getSchool().getSchoolName())
                .studentCode(student.getStudentCode())
                .className(student.getClassName())
                .gradeLevel(student.getGradeLevel())
                .build();
    }

    private AdminUserListResponse mapParentToAdminUserResponse(Parent parent) {
        User user = parent.getUser();
        // Lấy tên trường từ học sinh đầu tiên được liên kết (phụ huynh có thể có nhiều con ở nhiều trường)
        List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
        String schoolName = links.isEmpty() ? null : links.get(0).getStudent().getSchool().getSchoolName();

        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .displayName(parent.getFullName())
                .phoneNumber(parent.getPhoneNumber())
                .schoolName(schoolName)
                .build();
    }
}
