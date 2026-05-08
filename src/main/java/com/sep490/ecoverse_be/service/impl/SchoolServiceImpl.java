package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.StudentInformationRequest;
import com.sep490.ecoverse_be.dto.response.ListParentResponse;
import com.sep490.ecoverse_be.dto.response.ListStudentResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.StudentProfileResponse;
import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Student;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.Gender;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.ParentRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IProfileService;
import com.sep490.ecoverse_be.service.ISchoolService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SchoolServiceImpl implements ISchoolService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IProfileService profileService;
    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    @Override
    @Transactional
    public void softDeleteStudent(UUID schoolUserId, UUID studentId) {
        School school = schoolRepository.findByUserId(schoolUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh với id: " + studentId));

        if (!student.getSchool().getId().equals(school.getId())) {
            throw new BadRequestException("Học sinh không thuộc trường của bạn");
        }

        studentParentLinkRepository.deleteByStudentId(studentId);

        User studentUser = student.getUser();
        studentUser.setStatus(AccountStatus.INACTIVE);
        studentUser.setIsActive(false);
        userRepository.save(studentUser);
    }

    @Override
    public PageResponse<ListStudentResponse> getAllStudent(String keyword, Pageable pageable) {
        UUID userId = getCurrentUserId();
        School school = schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("school").get("id"), school.getId()));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("studentCode")), pattern);
                Predicate classLike = cb.like(cb.lower(root.get("className")), pattern);
                predicates.add(cb.or(nameLike, codeLike, classLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Student> page = studentRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::getListStudentResponse);
    }

    @Override
    public PageResponse<ListParentResponse> getAllParent(String keyword, Pageable pageable) {
        UUID userId = getCurrentUserId();
        School school = schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        // Find parents who have children in this school via StudentParentLink subquery
        Specification<Parent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Subquery: parent must have at least one StudentParentLink whose student belongs to this school
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<StudentParentLink> linkRoot = subquery.from(StudentParentLink.class);
            Join<StudentParentLink, Student> studentJoin = linkRoot.join("student");
            subquery.select(linkRoot.get("parent").get("id"))
                    .where(cb.equal(studentJoin.get("school").get("id"), school.getId()));

            predicates.add(root.get("id").in(subquery));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate phoneLike = cb.like(cb.lower(root.get("phoneNumber")), pattern);
                predicates.add(cb.or(nameLike, phoneLike));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Parent> page = parentRepository.findAll(spec, pageable);
        return PageResponse.from(page, SchoolServiceImpl::getListParentResponse);
    }

    @Override
    public void updateStudentStatus(UUID studentId, boolean isActive) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        User user = userRepository.findById(student.getUser().getId())
                .orElseThrow(() ->new RuntimeException("Không tìm thấy tài khoản"));

        if(isActive){
            user.setStatus(AccountStatus.ACTIVE);
            user.setIsActive(true);
        }else{
            user.setStatus(AccountStatus.SUSPENDED);
            user.setIsActive(false);
        }
        userRepository.save(user);
    }


    @Override
    @Transactional
    public StudentProfileResponse updateStudentInformation(UUID studentId, StudentInformationRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new  NotFoundException("Không tìm thấy học sinh"));

        if (request.getGender() != null) {
            student.setGender(Gender.valueOf(request.getGender()));
        }

        if (request.getFullName() != null) {
            student.setFullName(request.getFullName());
        }
        if (request.getStudentCode() != null) {
            student.setStudentCode(request.getStudentCode());
        }
        if (request.getGradeLevel() != null) {
            student.setGradeLevel(request.getGradeLevel());
        }
        if (request.getClassName() != null) {
            student.setClassName(request.getClassName());
        }
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getParentFullName() != null || request.getParentEmail() != null || request.getParentPhoneNumber() != null) {
            studentParentLinkRepository.findFirstByStudentId(studentId).ifPresent(link -> {
                Parent parent = link.getParent();
                if (request.getParentFullName() != null) {
                    parent.setFullName(request.getParentFullName());
                }
                if (request.getParentPhoneNumber() != null) {
                    parent.setPhoneNumber(request.getParentPhoneNumber());
                }
                if (request.getParentEmail() != null) {
                    parent.getUser().setEmail(request.getParentEmail());
                    userRepository.save(parent.getUser());
                }
                parentRepository.save(parent);
            });
        }

        student.setUpdatedAt(OffsetDateTime.now());
        Student updateStudent = studentRepository.save(student);
        return profileService.buildStudentProfileResponse(updateStudent);
    }

    @NotNull
    private static ListParentResponse getListParentResponse(Parent parent) {
        return ListParentResponse.builder()
                .parentId(parent.getId())
                .fullName(parent.getFullName())
                .phoneNumber(parent.getPhoneNumber())
                .parentEmail(parent.getUser().getEmail())
                .build();
    }

    @NotNull
    private ListStudentResponse getListStudentResponse(Student student) {
        return ListStudentResponse.builder()
                .studentId(student.getId())
                .studentFullName(student.getFullName())
                .studentCode(student.getStudentCode())
                .className(student.getClassName())
                .gradeLevel(student.getGradeLevel())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender().name())
                .address((student.getAddress()))
                .avatarUrl(student.getAvatarUrl())
                .avatarPresignedUrl(s3PresignedUrlService.generatePresignedUrl(student.getAvatarUrl()))
                .build();
    }
}
