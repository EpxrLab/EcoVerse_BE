package com.sep490.ecoverse_be.service;

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
}
