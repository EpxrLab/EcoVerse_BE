package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Student;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.ISchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public void softDeleteStudent(UUID schoolUserId, UUID studentId) {
        // Lấy thông tin trường dựa vào userId của tài khoản đang đăng nhập
        School school = schoolRepository.findByUserId(schoolUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));

        // Lấy thông tin học sinh cần xóa
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh với id: " + studentId));

        // Kiểm tra học sinh có thuộc trường này không
        if (!student.getSchool().getId().equals(school.getId())) {
            throw new BadRequestException("Học sinh không thuộc trường của bạn");
        }

        // Xóa toàn bộ liên kết StudentParentLink của học sinh này
        // (giữ nguyên tài khoản phụ huynh, chỉ hủy liên kết)
        studentParentLinkRepository.deleteByStudentId(studentId);

        // Vô hiệu hóa tài khoản User của học sinh (soft delete)
        User studentUser = student.getUser();
        studentUser.setStatus(AccountStatus.INACTIVE);
        studentUser.setIsActive(false);
        userRepository.save(studentUser);
    }
}
