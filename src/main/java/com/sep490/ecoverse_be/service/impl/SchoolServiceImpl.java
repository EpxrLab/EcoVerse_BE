package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.StudentInformationRequest;
import com.sep490.ecoverse_be.dto.response.ListParentResponse;
import com.sep490.ecoverse_be.dto.response.ListStudentResponse;
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
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.ISchoolService;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SchoolServiceImpl implements ISchoolService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;

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
    public List<ListStudentResponse> getAllStudent(){
        UUID userId = getCurrentUserId();
        School school = schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        List<Student> students = studentRepository.findBySchoolId(school.getId());
        List<ListStudentResponse> listStudentResponse = new ArrayList<>();

        for(Student student: students){
            ListStudentResponse dto = getListStudentResponse(student);
            listStudentResponse.add(dto);
        }
        return listStudentResponse;
    }

    @Override
    public List<ListParentResponse> getAllParent() {
        UUID userId = getCurrentUserId();
        School school = schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        List<Student> students = studentRepository.findBySchoolId(school.getId());

        Map<UUID, Parent> parentMap = new LinkedHashMap<>();
        for (Student student : students) {
            List<StudentParentLink> links = studentParentLinkRepository.findByStudentId(student.getId());
            for (StudentParentLink link : links) {
                Parent parent = link.getParent();
                parentMap.putIfAbsent(parent.getId(), parent);
            }
        }

        // Map từng Parent sang DTO ListParentResponse
        List<ListParentResponse> result = new ArrayList<>();
        for (Parent parent : parentMap.values()) {
            result.add(getListParentResponse(parent));
        }
        return result;
    }

    @Override
    public void updateStudentStatus(UUID studentId, boolean isActive) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        User user = userRepository.findById(student.getUser().getId())
                .orElseThrow(() ->new RuntimeException("Không tìm thấy tài khoản"));

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
                .orElseThrow(() -> new  NotFoundException("Không tìm thấy học sinh"));

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
        student.setUpdatedAt(LocalDateTime.now());
        Student updateStudent = studentRepository.save(student);
        StudentProfileResponse response = modelMapper.map(updateStudent, StudentProfileResponse.class);
        response.setId(updateStudent.getId());
        response.setAvatarUrl(updateStudent.getAvatarUrl());
        response.setTotalCoins(updateStudent.getTotalCoins());
        response.setIsFirstLogin(updateStudent.getIsFirstLogin());
        return response;
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
    private static ListStudentResponse getListStudentResponse(Student student) {
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
                .build();
    }
}
