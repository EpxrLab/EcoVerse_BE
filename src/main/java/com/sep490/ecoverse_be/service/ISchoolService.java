package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.StudentInformationRequest;
import com.sep490.ecoverse_be.dto.response.ListParentResponse;
import com.sep490.ecoverse_be.dto.response.ListStudentResponse;
import com.sep490.ecoverse_be.dto.response.StudentProfileResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service xử lý các chức năng quản lý của trường học.
 */
public interface ISchoolService {

    /**
     * Xóa mềm học sinh khỏi trường.
     * Vô hiệu hóa tài khoản user của học sinh và xóa liên kết StudentParentLink.
     * Chỉ trường sở hữu mới được xóa học sinh của mình.
     *
     * @param schoolUserId userId của tài khoản trường đang đăng nhập
     * @param studentId    id của bản ghi Student cần xóa
     */
    void softDeleteStudent(UUID schoolUserId, UUID studentId);

    List<ListStudentResponse> getAllStudent();

    /**
     * Lấy danh sách phụ huynh có con em đang học tại trường.
     * Deduplicate theo parentId để tránh phụ huynh có nhiều con bị lặp.
     */
    List<ListParentResponse> getAllParent();

    StudentProfileResponse updateStudentInformation(UUID studentId, StudentInformationRequest request);
}
