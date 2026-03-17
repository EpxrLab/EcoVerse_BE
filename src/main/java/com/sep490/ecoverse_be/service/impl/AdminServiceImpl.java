package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ParentAdminDetail;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.dto.response.StudentAdminDetail;
import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Student;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.PaymentMethod;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.ParentRepository;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.PaymentRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.SubscriptionPlanRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IAdminService;
import com.sep490.ecoverse_be.service.IEmailService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User getCurrentAdmin() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUser();
    }

    @Override
    public PageResponse<SchoolDetailResponse> getPendingSchools(String keyword, Pageable pageable) {
        Specification<School> spec = buildSchoolSpec(ApprovalStatus.PENDING, keyword);
        Page<School> page = schoolRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToSchoolDetailResponse);
    }

    @Override
    public PageResponse<PartnershipDetailResponse> getPendingPartnerships(String keyword, Pageable pageable) {
        Specification<Partnership> spec = buildPartnershipSpec(ApprovalStatus.PENDING, keyword);
        Page<Partnership> page = partnershipRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToPartnershipDetailResponse);
    }

    @Override
    public PageResponse<SchoolDetailResponse> getApprovedSchools(String keyword, Pageable pageable) {
        Specification<School> spec = buildSchoolSpec(ApprovalStatus.APPROVED, keyword);
        Page<School> page = schoolRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToSchoolDetailResponse);
    }

    @Override
    public PageResponse<PartnershipDetailResponse> getApprovedPartnerships(String keyword, Pageable pageable) {
        Specification<Partnership> spec = buildPartnershipSpec(ApprovalStatus.APPROVED, keyword);
        Page<Partnership> page = partnershipRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToPartnershipDetailResponse);
    }

    @Override
    public PageResponse<SchoolDetailResponse> getAllSchools(String keyword, Pageable pageable) {
        Specification<School> spec = buildSchoolSpec(null, keyword);
        Page<School> page = schoolRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToSchoolDetailResponse);
    }

    @Override
    public PageResponse<PartnershipDetailResponse> getAllPartnerships(String keyword, Pageable pageable) {
        Specification<Partnership> spec = buildPartnershipSpec(null, keyword);
        Page<Partnership> page = partnershipRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToPartnershipDetailResponse);
    }

    @Override
    public PageResponse<AdminUserListResponse> getAllUsers(Role role, UUID schoolId, String keyword, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by role
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            } else {
                // Only return these 4 roles (exclude ADMINISTRATOR)
                predicates.add(root.get("role").in(
                        Role.PARTNERSHIP_SCHOOL,
                        Role.THIRD_PARTY_PARTNERSHIP,
                        Role.STUDENT,
                        Role.PARENT
                ));
            }

            // Keyword search on email/username
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                Predicate usernameLike = cb.like(cb.lower(root.get("username")), pattern);
                predicates.add(cb.or(emailLike, usernameLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec, pageable);
        return PageResponse.from(page, user -> mapUserToAdminUserResponse(user, schoolId));
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

            assignFreeSubscription("SCHOOL_FREE", SubscriberType.SCHOOL, school, null, schoolUser);

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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác với id: " + id));

        if (partnership.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Đối tác không ở trạng thái chờ duyệt.");
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

            assignFreeSubscription("PARTNERSHIP_FREE", SubscriberType.PARTNERSHIP, null, partnership, schoolUser);

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
                .logoUrl(s3PresignedUrlService.generatePresignedUrl(school.getLogoUrl()))
                .licenseUrl(s3PresignedUrlService.generatePresignedUrl(school.getLicenseUrl()))
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
                .logoUrl(s3PresignedUrlService.generatePresignedUrl(partnership.getLogoUrl()))
                .licenseUrl(s3PresignedUrlService.generatePresignedUrl(partnership.getLicenseUrl()))
                .approvalStatus(partnership.getApprovalStatus())
                .approvedAt(partnership.getApprovedAt())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(partnership.getCreatedAt())
                .updatedAt(partnership.getUpdatedAt())
                .build();
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

    // --- Specification builders ---

    private Specification<School> buildSchoolSpec(ApprovalStatus approvalStatus, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("schoolName")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("contactEmail")), pattern);
                Predicate taxCodeLike = cb.like(cb.lower(root.get("taxCode")), pattern);
                predicates.add(cb.or(nameLike, emailLike, taxCodeLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Partnership> buildPartnershipSpec(ApprovalStatus approvalStatus, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("organizationName")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("contactEmail")), pattern);
                Predicate taxCodeLike = cb.like(cb.lower(root.get("taxCode")), pattern);
                predicates.add(cb.or(nameLike, emailLike, taxCodeLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // --- User mapping helper for paginated getAllUsers ---

    private AdminUserListResponse mapUserToAdminUserResponse(User user, UUID schoolIdFilter) {
        return switch (user.getRole()) {
            case PARTNERSHIP_SCHOOL -> {
                School school = schoolRepository.findByUserId(user.getId()).orElse(null);
                yield school != null ? mapSchoolToAdminUserResponse(school) : buildBasicResponse(user);
            }
            case THIRD_PARTY_PARTNERSHIP -> {
                Partnership partnership = partnershipRepository.findByUserId(user.getId()).orElse(null);
                yield partnership != null ? mapPartnershipToAdminUserResponse(partnership) : buildBasicResponse(user);
            }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                if (student != null && schoolIdFilter != null && !student.getSchool().getId().equals(schoolIdFilter)) {
                    yield null; // filtered out by schoolId
                }
                yield student != null ? mapStudentToAdminUserResponse(student) : buildBasicResponse(user);
            }
            case PARENT -> {
                Parent parent = parentRepository.findByUserId(user.getId()).orElse(null);
                if (parent != null && schoolIdFilter != null) {
                    List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
                    boolean belongsToSchool = links.stream()
                            .anyMatch(link -> link.getStudent().getSchool().getId().equals(schoolIdFilter));
                    if (!belongsToSchool) {
                        yield null; // filtered out by schoolId
                    }
                }
                yield parent != null ? mapParentToAdminUserResponse(parent) : buildBasicResponse(user);
            }
            default -> buildBasicResponse(user);
        };
    }

    private AdminUserListResponse buildBasicResponse(User user) {
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(null)
                .build();
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
                .avatarUrl(s3PresignedUrlService.generatePresignedUrl(student.getAvatarUrl()))
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

    private void assignFreeSubscription(String planCode, SubscriberType subscriberType,
                                         School school, Partnership partnership, User user) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCode(planCode).orElse(null);
        if (plan == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = new Subscription();
        subscription.setSubscriptionCode("SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        subscription.setSubscriberType(subscriberType);
        subscription.setSchool(school);
        subscription.setPartnership(partnership);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusDays(plan.getDurationDays()));
        subscriptionRepository.save(subscription);

        Payment payment = new Payment();
        payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setSubscriberType(subscriberType);
        payment.setSchool(school);
        payment.setPartnership(partnership);
        payment.setSubscription(subscription);
        payment.setAmount(BigDecimal.ZERO);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.OTHER);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(now);
        payment.setPayerName(user.getEmail());
        payment.setPayerEmail(user.getEmail());
        payment.setCreatedBy(user);
        payment.setNotes("Free plan - auto-assigned on approval");
        paymentRepository.save(payment);
    }
}
