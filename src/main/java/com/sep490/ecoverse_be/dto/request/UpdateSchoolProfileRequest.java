package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.SchoolType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body cho API cập nhật hồ sơ trường học.
 * Các trường bị cấm chỉnh sửa: taxCode, logoUrl, licenseUrl.
 */
@Getter
@Setter
public class UpdateSchoolProfileRequest {

    @Size(max = 255, message = "Tên trường tối đa 255 ký tự")
    private String schoolName;

    private SchoolType schoolType;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Size(max = 100, message = "Quận/huyện tối đa 100 ký tự")
    private String district;

    @Size(max = 100, message = "Tỉnh/thành tối đa 100 ký tự")
    private String province;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số, bắt đầu bằng 0)")
    private String phoneNumber;

    @Size(max = 255, message = "Tên hiệu trưởng tối đa 255 ký tự")
    private String principalName;

    @Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
    private String position;

    @Email(message = "Email liên hệ không hợp lệ")
    private String contactEmail;

    @Size(max = 100, message = "Link website tối đa 100 ký tự")
    private String linkWeb;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;
}
