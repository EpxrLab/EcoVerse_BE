package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.StudentInformationRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSchoolProfileRequest;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ISchoolService;
import com.sep490.ecoverse_be.service.IStudentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/school/")
@Tag(name = "School - Student Management", description = "APIs quản lý học sinh dành cho Trường học (import, xem danh sách, xóa)")
@PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
public class ImportFileStudentController {

    @Autowired
    private IStudentImportService studentImportService;

    @Autowired
    private ISchoolService schoolService;

    @PostMapping(value = "/import/account", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import file Excel tạo tài khoản học sinh và phụ huynh",
            description = """
                    Upload file Excel (`.xlsx`) để tự động tạo tài khoản cho học sinh và phụ huynh.
                    Yêu cầu Bearer token của tài khoản **Trường học** (PARTNERSHIP_SCHOOL).

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: multipart/form-data
                    ```

                    **Form field:** `file` — file Excel `.xlsx`

                    **Cấu trúc file Excel (các cột theo đúng thứ tự, hàng đầu tiên là header):**

                    | Student Full Name | Class Name | Grade Level | Date of Birth | Gender | Parent Full Name | Parent Phone Number | Parent Email |
                    |---|---|---|---|---|---|---|---|
                    | Nguyễn Hoàng Nhật Ân | 5A | 5 | 2015-03-20 | Male | Nguyễn Văn A | 0905324995 | parent@gmail.com |

                    - `Date of Birth`: định dạng `YYYY-MM-DD`
                    - `Gender`: `Male` hoặc `Female`
                    - `Parent Phone Number`: 10 chữ số, bắt đầu bằng `0`

                    **Tài khoản được tạo:**
                    - **Học sinh:** đăng nhập bằng `studentCode` (tự động sinh, VD: `AnNHN`) + mật khẩu ngẫu nhiên 8 ký tự
                    - **Phụ huynh:** đăng nhập bằng `số điện thoại` + mật khẩu ngẫu nhiên 8 ký tự
                    - Nếu số điện thoại phụ huynh đã tồn tại → tái sử dụng tài khoản phụ huynh cũ

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Import hoàn tất: 30 thành công, 2 thất bại",
                      "data": {
                        "totalRows": 32,
                        "successCount": 30,
                        "failCount": 2,
                        "errors": [
                          { "rowNumber": 5, "field": "Parent Email", "message": "Email không hợp lệ" }
                        ]
                      }
                    }
                    ```
                    """
    )
    public ResponseDto<ImportResultResponse> importStudents(
            @RequestParam("file") MultipartFile file) {
        ImportResultResponse result = studentImportService.importStudentsFromExcel(file);

        return new ResponseDto<>(HttpStatus.OK.value(),
                "Import hoàn tất: " + result.getSuccessCount() + " thành công, " + result.getFailCount() + " thất bại",
                result);
    }

    @GetMapping("/accounts")
    @Operation(
            summary = "Lấy danh sách thông tin đăng nhập học sinh và phụ huynh",
            description = """
                    Lấy toàn bộ danh sách tài khoản học sinh và phụ huynh của trường đang đăng nhập.
                    Kết quả được nhóm theo phụ huynh — mỗi phụ huynh chứa danh sách con.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy danh sách thành công",
                      "data": {
                        "schoolName": "Trường Tiểu Học Lê Văn Tám",
                        "totalStudents": 30,
                        "totalParents": 25,
                        "accounts": [
                          {
                            "parentFullName": "Nguyễn Văn A",
                            "phoneNumber": "0905324995",
                            "password": "********",
                            "parentEmail": "parent@gmail.com",
                            "children": [
                              {
                                "studentFullName": "Nguyễn Hoàng Nhật Ân",
                                "studentCode": "AnNHN",
                                "password": "********",
                                "className": "5A",
                                "gradeLevel": "5"
                              }
                            ]
                          }
                        ]
                      }
                    }
                    ```

                    > Mật khẩu hiển thị là `********`. Để lấy mật khẩu thực, dùng API **Send Credentials** để reset và gửi email.
                    """
    )
    public ResponseDto<AccountListResponse> getAccounts() {
        AccountListResponse accounts = studentImportService.getImportedAccounts();

        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy danh sách thành công", accounts);
    }

    @PostMapping("/send-credentials")
    @Operation(
            summary = "Gửi email thông tin đăng nhập cho phụ huynh",
            description = """
                    Reset mật khẩu mới cho toàn bộ học sinh và phụ huynh của trường, sau đó gửi email thông tin đăng nhập tới từng phụ huynh.

                    - Mỗi phụ huynh nhận **1 email** chứa:
                      - Thông tin đăng nhập của chính phụ huynh (số điện thoại + mật khẩu mới)
                      - Bảng thông tin đăng nhập của tất cả con em (student code + mật khẩu mới)
                    - **Lưu ý:** API này sẽ **reset mật khẩu** — học sinh/phụ huynh cần dùng mật khẩu mới được gửi qua email.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseDto<String> sendCredentials() {
        studentImportService.sendCredentialEmails();
        return new ResponseDto<>(HttpStatus.OK.value(), "Đã gửi email thông tin đăng nhập cho tất cả phụ huynh", null);
    }

    @DeleteMapping("/students/{studentId}")
    @Operation(
            summary = "Xóa mềm học sinh khỏi trường",
            description = """
                    Vô hiệu hóa tài khoản học sinh (soft delete) và hủy toàn bộ liên kết phụ huynh-học sinh.
                    Chỉ dành cho role **PARTNERSHIP_SCHOOL**. Trường chỉ được xóa học sinh của chính mình.

                    **Hành vi xóa mềm:**
                    - Tài khoản User của học sinh bị chuyển sang trạng thái `INACTIVE` và `isActive = false`
                    - Toàn bộ bản ghi `StudentParentLink` của học sinh bị xóa
                    - Tài khoản phụ huynh **không bị ảnh hưởng** — phụ huynh vẫn tồn tại, chỉ mất liên kết với học sinh này
                    - Dữ liệu học sinh (điểm số, lịch sử game,...) được giữ lại trong DB

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Path variable:** `studentId` — UUID của bản ghi Student (không phải userId)

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Xóa học sinh thành công",
                      "data": null
                    }
                    ```

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy học sinh
                    - `400` — Học sinh không thuộc trường của bạn
                    """
    )
    public ResponseDto<Void> deleteStudent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID studentId) {
        schoolService.softDeleteStudent(principal.getUser().getId(), studentId);
        return new ResponseDto<>(HttpStatus.OK.value(), "Xóa học sinh thành công", null);
    }

    @GetMapping("/students")
    @Operation(
            summary = "Lấy danh sách thông tin học sinh",
            description = """
                    Lấy toàn bộ danh sách học sinh.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy danh sách thành công",
                      "data": {
                                "studentId": "uuid"
                                "studentFullName": "Nguyễn Hoàng Nhật Ân",
                                "studentCode": "AnNHN",
                                "className": "5A",
                                "gradeLevel": "5",
                                "academicYear": "2025-2026",
                                "dateOfBirth": "2015-03-20",
                                "gender": "MALE",
                                "address": "Lương Định Của",
                                "avatar": "https://cloudinary.com/avatar.png"
                               }
                    }
                    """
    )
    public ResponseDto<List<ListStudentResponse>> getStudents() {
        List<ListStudentResponse> studentResponse = schoolService.getAllStudent();
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy danh sách thành công", studentResponse);
    }

    @GetMapping("/parents")
    @Operation(
            summary = "Lấy danh sách thông tin phụ huynh",
            description = """
                    Lấy toàn bộ danh sách phụ huynh.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Lấy danh sách thành công",
                      "data": {
                                "parentId": "uuid"
                                "fullName": "Nguyễn Văn Quốc",
                                "phoneNumber": "0905324995",
                                "parentEmail": "nguyenhoangnhatan31@gmail.com"
                               }
                    }
                    """
    )
    public ResponseDto<List<ListParentResponse>> getParents() {
        List<ListParentResponse> parentResponse = schoolService.getAllParent();
        return new ResponseDto<>(HttpStatus.OK.value(), "Lấy danh sách thành công", parentResponse);
    }

    @PutMapping("/update/students/{studentId}")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Cập nhật hồ sơ học sinh",
            description = """
                    Cập nhật thông tin hồ sơ của học sinh. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    **Hỗ trợ partial update:** chỉ gửi các trường cần thay đổi, các trường không gửi sẽ giữ nguyên.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body mẫu:**
                    ```json
                    {
                      "fullName": "Nguyễn Hoàng Nhật Ân",
                      "studentCode": "AnNHN",
                      "className": "A2",
                      "gradeLevel": "1",
                      "dateOfBirth": "2004-08-20",
                      "gender": "MALE",
                      "address": "Lương Định Của"
                    }
                    """
    )
    public ResponseDto<StudentProfileResponse> updateStudentProfileBySchool(
            @RequestParam UUID studentId,
            @Valid @RequestBody StudentInformationRequest request) {
        StudentProfileResponse response = schoolService.updateStudentInformation(studentId, request);
        return new ResponseDto<>(HttpStatus.OK.value(), "Cập nhật hồ sơ trường học thành công", response);
    }
}
