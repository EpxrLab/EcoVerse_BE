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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
                    """
    )
    public ResponseEntity<ResponseDto<ImportResultResponse>> importStudents(
            @RequestParam("file") MultipartFile file) {
        ImportResultResponse result = studentImportService.importStudentsFromExcel(file);
        return ResponseEntity.ok(ResponseDto.success(result,
                "Import hoàn tất: " + result.getSuccessCount() + " thành công, " + result.getFailCount() + " thất bại"));
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
                    """
    )
    public ResponseEntity<ResponseDto<AccountListResponse>> getAccounts() {
        AccountListResponse accounts = studentImportService.getImportedAccounts();
        return ResponseEntity.ok(ResponseDto.success(accounts, "Lấy danh sách thành công"));
    }

    @PostMapping("/add-student")
    @Operation(
            summary = "Thêm học sinh và phụ huynh thủ công",
            description = """
                Thêm một học sinh và phụ huynh vào hệ thống bằng cách nhập thủ công (không thông qua file Excel).
                Email thông tin đăng nhập sẽ **chưa được gửi ngay** — cần gọi API `/send-credentials` riêng.

                - `studentCode` sẽ được **tự động sinh** dựa trên tên của học sinh.
                - Nếu **số điện thoại của phụ huynh đã tồn tại** → hệ thống sẽ **tái sử dụng tài khoản phụ huynh hiện có**.

                **Header:**
                ```
                Authorization: Bearer <accessToken>
                Content-Type: application/json
                ```
                """
    )
    public ResponseEntity<ResponseDto<Void>> addStudentManually(@Valid @RequestBody AddStudentManualRequest request) {
        studentImportService.addStudentManually(request);
        return ResponseEntity.ok(ResponseDto.success(null, "Thêm học sinh thành công. Vui lòng gửi email sau."));
    }

    @PostMapping("/send-credentials")
    @Operation(
            summary = "Gửi email thông tin đăng nhập cho các phụ huynh chưa nhận",
            description = """
                Chỉ gửi email cho các phụ huynh có `credentialEmailSent = false` (chưa từng nhận email).
                Các phụ huynh đã nhận email trước đó sẽ bị **bỏ qua** để tránh gửi trùng lần 2.

                - Mỗi phụ huynh sẽ nhận **1 email** chứa thông tin đăng nhập của họ và các học sinh con.
                - Mật khẩu sẽ được **reset mới** cho cả phụ huynh và học sinh khi gửi email.

                **Header:**
                ```
                Authorization: Bearer <accessToken>
                ```
                """
    )
    public ResponseEntity<ResponseDto<SendCredentialSummaryResponse>> sendCredentials() {
        SendCredentialSummaryResponse summary = studentImportService.sendCredentialEmails();
        return ResponseEntity.ok(ResponseDto.success(summary,
                "Đã gửi " + summary.getSentCount() + " email. Bỏ qua " + summary.getSkippedCount() + " phụ huynh đã nhận."));
    }

    @PostMapping("/resend-credentials/{parentId}")
    @Operation(
            summary = "Gửi lại email cho một phụ huynh cụ thể",
            description = """
                Reset mật khẩu và gửi lại email thông tin đăng nhập cho **1 phụ huynh cụ thể**.

                - Mật khẩu của phụ huynh và các học sinh con sẽ được **reset mới**.
                - Sau khi gửi xong, `credentialEmailSent` của phụ huynh sẽ được đặt lại thành `true`.

                **Path variable:** `parentId` — UUID của bản ghi Parent

                **Header:**
                ```
                Authorization: Bearer <accessToken>
                ```
                """
    )
    public ResponseEntity<ResponseDto<Void>> resendCredentials(@PathVariable UUID parentId) {
        studentImportService.resendCredentialEmail(parentId);
        return ResponseEntity.ok(ResponseDto.success(null, "Đã gửi lại email thành công"));
    }

    @DeleteMapping("/students/{studentId}")
    @Operation(
            summary = "Xóa mềm học sinh khỏi trường",
            description = """
                    Vô hiệu hóa tài khoản học sinh (soft delete) và hủy toàn bộ liên kết phụ huynh-học sinh.
                    Chỉ dành cho role **PARTNERSHIP_SCHOOL**. Trường chỉ được xóa học sinh của chính mình.

                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy học sinh
                    - `400` — Học sinh không thuộc trường của bạn
                    """
    )
    public ResponseEntity<ResponseDto<Void>> deleteStudent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID studentId) {
        schoolService.softDeleteStudent(principal.getUser().getId(), studentId);
        return ResponseEntity.ok(ResponseDto.success(null, "Xóa học sinh thành công"));
    }

    @GetMapping("/students")
    @Operation(
            summary = "Lấy danh sách thông tin học sinh",
            description = """
                    Lấy toàn bộ danh sách học sinh của trường đang đăng nhập.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<List<ListStudentResponse>>> getStudents() {
        List<ListStudentResponse> studentResponse = schoolService.getAllStudent();
        return ResponseEntity.ok(ResponseDto.success(studentResponse, "Lấy danh sách thành công"));
    }

    @GetMapping("/parents")
    @Operation(
            summary = "Lấy danh sách thông tin phụ huynh",
            description = """
                    Lấy toàn bộ danh sách phụ huynh của trường đang đăng nhập.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<List<ListParentResponse>>> getParents() {
        List<ListParentResponse> parentResponse = schoolService.getAllParent();
        return ResponseEntity.ok(ResponseDto.success(parentResponse, "Lấy danh sách thành công"));
    }

    @PutMapping("/update/students/{studentId}")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(
            summary = "Cập nhật hồ sơ học sinh",
            description = """
                    Cập nhật thông tin hồ sơ của học sinh. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    **Hỗ trợ partial update:** chỉ gửi các trường cần thay đổi.

                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<StudentProfileResponse>> updateStudentProfileBySchool(
            @RequestParam UUID studentId,
            @Valid @RequestBody StudentInformationRequest request) {
        StudentProfileResponse response = schoolService.updateStudentInformation(studentId, request);
        return ResponseEntity.ok(ResponseDto.success(response, "Cập nhật hồ sơ học sinh thành công"));
    }

    @PutMapping("/de-active/student/{studentId}")
    @Operation(
            summary = "Kích hoạt hoặc khóa tài khoản học sinh",
            description = """
                    Bật/tắt trạng thái hoạt động của một tài khoản học sinh. Chỉ dành cho role **PARTNERSHIP_SCHOOL**.

                    - `true` → Kích hoạt: `status = ACTIVE`, `isActive = true`
                    - `false` → Khóa: `status = INACTIVE`, `isActive = false`

                    **Request body:** `true` hoặc `false` (boolean thuần)
                    """
    )
    public ResponseEntity<ResponseDto<String>> blockUser(
            @PathVariable UUID studentId,
            @RequestBody boolean isActive) {
        schoolService.updateStudentStatus(studentId, isActive);
        return ResponseEntity.ok(ResponseDto.success(null, "Thành công"));
    }
}
