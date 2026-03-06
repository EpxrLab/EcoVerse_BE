package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.AccountListResponse;
import com.sep490.ecoverse_be.dto.response.ImportResultResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.IStudentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/school/")
@Tag(name = "School - Student Import", description = "APIs quản lý import tài khoản học sinh và phụ huynh (dành cho Trường học)")
@PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
public class ImportFileStudentController {

    @Autowired
    private IStudentImportService studentImportService;

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
}
