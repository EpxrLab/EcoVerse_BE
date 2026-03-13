package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.AddStudentManualRequest;
import com.sep490.ecoverse_be.dto.request.StudentInformationRequest;
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

    @PostMapping("/add-student")
    @Operation(
            summary = "Thêm học sinh và phụ huynh thủ công",
            description = """
                Thêm một học sinh và phụ huynh vào hệ thống bằng cách nhập thủ công (không thông qua file Excel).
                Email thông tin đăng nhập sẽ **chưa được gửi ngay** — cần gọi API `/send-credentials` riêng để gửi email.

                - `studentCode` sẽ được **tự động sinh** dựa trên tên của học sinh.
                - Nếu **số điện thoại của phụ huynh đã tồn tại** → hệ thống sẽ **tái sử dụng tài khoản phụ huynh hiện có**, 
                  chỉ thêm liên kết với học sinh mới.

                **Header:**
                ```
                Authorization: Bearer <accessToken>
                Content-Type: application/json
                ```
                """
    )
    public ResponseDto<Void> addStudentManually(@Valid @RequestBody AddStudentManualRequest request) {
        studentImportService.addStudentManually(request);
        return new ResponseDto<>(HttpStatus.OK.value(), "Thêm học sinh thành công. Vui lòng gửi email sau.", null);
    }

    @PostMapping("/send-credentials")
    @Operation(
            summary = "Gửi email thông tin đăng nhập cho các phụ huynh chưa nhận",
            description = """
                Chỉ gửi email cho các phụ huynh có `credentialEmailSent = false` (chưa từng nhận email).
                Các phụ huynh đã nhận email trước đó sẽ bị **bỏ qua** để tránh gửi trùng lần 2.

                - Mỗi phụ huynh sẽ nhận **1 email** chứa thông tin đăng nhập của họ và các học sinh con.
                - Mật khẩu sẽ được **reset mới** cho cả phụ huynh và học sinh khi gửi email.

                **Response trả về:**
                - `sentCount`: số phụ huynh được gửi email trong lần này
                - `skippedCount`: số phụ huynh bị bỏ qua (đã nhận email trước đó)

                **Header:**
                ```
                Authorization: Bearer <accessToken>
                ```
                """
    )
    public ResponseDto<SendCredentialSummaryResponse> sendCredentials() {
        SendCredentialSummaryResponse summary = studentImportService.sendCredentialEmails();
        return new ResponseDto<>(HttpStatus.OK.value(),
                "Đã gửi " + summary.getSentCount() + " email. Bỏ qua " + summary.getSkippedCount() + " phụ huynh đã nhận.",
                summary);
    }

    @PostMapping("/resend-credentials/{parentId}")
    @Operation(
            summary = "Gửi lại email cho một phụ huynh cụ thể",
            description = """
                Reset mật khẩu và gửi lại email thông tin đăng nhập cho **1 phụ huynh cụ thể**.
                Dùng khi phụ huynh báo chưa nhận được email ở lần gửi trước.

                - Mật khẩu của phụ huynh và các học sinh con sẽ được **reset mới**.
                - Sau khi gửi xong, `credentialEmailSent` của phụ huynh sẽ được đặt lại thành `true`.

                **Path variable:** `parentId` — UUID của bản ghi Parent

                **Header:**
                ```
                Authorization: Bearer <accessToken>
                ```
                """
    )
    public ResponseDto<Void> resendCredentials(@PathVariable UUID parentId) {
        studentImportService.resendCredentialEmail(parentId);
        return new ResponseDto<>(HttpStatus.OK.value(), "Đã gửi lại email thành công", null);
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
                                "avatar": "https://example.s3.amazonaws.com/avatar.png"
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

    @PutMapping("/de-active/student/{studentId}")
    @Operation(
            summary = "Kích hoạt hoặc khóa tài khoản học sinh",
            description = """
                    Bật/tắt trạng thái hoạt động của một tài khoản bất kỳ. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    - `true` → Kích hoạt: `status = ACTIVE`, `isActive = true`
                    - `false` → Khóa: `status = INACTIVE`, `isActive = false`

                    Dùng để block học sinh khi vi phạm.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```

                    **Request body:** `true` hoặc `false` (boolean thuần)

                    **Path variable:** `studentId` — UUID của bản ghi Student

                    **Response mẫu:**
                    ```json
                    {
                      "status": 200,
                      "message": "Thành công",
                      "data": null
                    }
                    ```
                    """
    )
    public ResponseDto<String> blockUser(@PathVariable UUID studentId, @RequestBody boolean isActive) {
        schoolService.updateStudentStatus(studentId, isActive);
        return ResponseDto.success(null, "Thành công");
    }
}
