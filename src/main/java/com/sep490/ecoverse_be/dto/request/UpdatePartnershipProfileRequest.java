package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.PartnershipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body cho API cập nhật hồ sơ đối tác.
 * Các trường bị cấm chỉnh sửa: taxCode, logoUrl, licenseUrl.
 */
@Getter
@Setter
public class UpdatePartnershipProfileRequest {

    @Size(max = 255, message = "Tên tổ chức tối đa 255 ký tự")
    private String organizationName;

    private PartnershipType partnershipType;

    @Email(message = "Email liên hệ không hợp lệ")
    private String contactEmail;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số, bắt đầu bằng 0)")
    private String phoneNumber;

    @Size(max = 1000, message = "Địa chỉ đăng ký tối đa 1000 ký tự")
    private String registeredAddress;

    @Size(max = 100, message = "Quận/huyện tối đa 100 ký tự")
    private String geographicScopeWard;

    @Size(max = 100, message = "Tỉnh/thành tối đa 100 ký tự")
    private String geographicScopeProvince;

    @Size(max = 255, message = "Tên người liên hệ tối đa 255 ký tự")
    private String contactPerson;

    @Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
    private String position;

    @Size(max = 100, message = "Link website tối đa 100 ký tự")
    private String linkWeb;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;
}
