package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.CreateQuizRequest;
import com.sep490.ecoverse_be.dto.request.QuizQuestionRequest;
import com.sep490.ecoverse_be.dto.request.UpdateQuizRequest;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.service.IQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quiz")
@PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
@Tag(name = "Quiz Management", description = "APIs quản lý quiz dành cho School và Partnership")
public class QuizController {

    @Autowired
    private IQuizService quizService;

    @PostMapping("/manual")
    @Operation(
            summary = "Tạo quiz thủ công",
            description = """
                    Tạo quiz mới với metadata và danh sách câu hỏi.
                    
                    **Flow khuyến nghị:**
                    1. (Tuỳ chọn) Upload file Excel qua `POST /preview-questions` để lấy preview câu hỏi
                    2. Gọi API này với metadata + `questions` (nhập tay hoặc từ kết quả preview)
                    
                    - `questions` là optional: để trống nếu muốn thêm câu hỏi sau.
                    - Mỗi câu hỏi phải có ít nhất 2 đáp án, trong đó ít nhất 1 đúng.
                    - `questionOrder` trong danh sách không được trùng nhau.
                    - Quiz sau khi tạo sẽ ở trạng thái **chưa publish** (`isPublished = false`).
                    
                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> createQuizManual(
            @Valid @RequestBody CreateQuizRequest request) {
        QuizResponse response = quizService.createQuizManual(request);
        return ResponseEntity.status(201).body(ResponseDto.created(response, "Tạo quiz thành công"));
    }

    @PostMapping(value = "/preview-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Preview câu hỏi từ file Excel (không ghi DB)",
            description = """
                    Upload file Excel (.xlsx) để backend parse và trả về danh sách câu hỏi dưới dạng JSON.
                    Không ghi vào database — dùng để UI hiển thị preview trước khi user xác nhận tạo quiz.
                    
                    **Cấu trúc file Excel** (header row bắt buộc, 7 cột theo đúng thứ tự):
                    | question_order | question_text | answer_A | answer_B | answer_C | answer_D | correct_answer |
                    
                    - `answer_C`, `answer_D`: không bắt buộc, để trống nếu không có
                    - `correct_answer`: **A / B / C / D**
                    
                    **Sau khi preview:** Gọi `POST /manual` với metadata + questions list đã xác nhận.
                    
                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: multipart/form-data
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<List<QuizQuestionRequest>>> previewQuestions(
            @RequestParam("file") MultipartFile file) {
        List<QuizQuestionRequest> questions = quizService.previewQuestionsFromExcel(file);
        return ResponseEntity.ok(ResponseDto.success(questions,
                "Parse thành công " + questions.size() + " câu hỏi từ file Excel"));
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách quiz của tôi",
            description = """
                Lấy toàn bộ danh sách quiz do tài khoản đang đăng nhập tạo (school hoặc partnership).
                Kết quả sắp xếp theo thời gian tạo mới nhất.
                
                **Header:**
                ```
                Authorization: Bearer <accessToken>
                ```
                """
    )
    public ResponseEntity<ResponseDto<List<QuizSummaryResponse>>> getMyQuizzes() {
        return ResponseEntity.ok(
                ResponseDto.success(quizService.getMyQuizzes(), "Lấy danh sách quiz thành công"));
    }

    @GetMapping("/{quizId}")
    @Operation(
            summary = "Lấy chi tiết quiz (bao gồm câu hỏi và đáp án)",
            description = """
                Lấy thông tin đầy đủ của một quiz bao gồm tất cả câu hỏi và đáp án.
                Chỉ trả về quiz do tài khoản hiện tại sở hữu.
                
                **Lỗi có thể xảy ra:**
                - `404` — Không tìm thấy quiz hoặc không có quyền truy cập
                """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> getQuizById(@PathVariable UUID quizId) {
        return ResponseEntity.ok(
                ResponseDto.success(quizService.getQuizById(quizId), "Lấy chi tiết quiz thành công"));
    }

    @PutMapping("/{quizId}")
    @Operation(
            summary = "Cập nhật thông tin quiz (partial update)",
            description = """
                Cập nhật các thông tin chung của quiz (tiêu đề, mô tả, độ khó, ...).
                Hỗ trợ partial update: chỉ cần gửi các trường cần thay đổi.
                
                **Lưu ý:** Không thể sửa quiz đã được publish. Vui lòng unpublish trước.
                
                **Lỗi có thể xảy ra:**
                - `400` — Quiz đang ở trạng thái published
                - `404` — Không tìm thấy quiz
                """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> updateQuiz(
            @PathVariable UUID quizId,
            @Valid @RequestBody UpdateQuizRequest request) {
        return ResponseEntity.ok(
                ResponseDto.success(quizService.updateQuiz(quizId, request), "Cập nhật quiz thành công"));
    }

    @DeleteMapping("/{quizId}")
    @Operation(
            summary = "Xóa quiz",
            description = """
                Soft-delete một quiz cùng toàn bộ câu hỏi và đáp án liên quan.
                Chỉ có thể xóa quiz do chính tài khoản tạo.
                
                **Lỗi có thể xảy ra:**
                - `404` — Không tìm thấy quiz
                """
    )
    public ResponseEntity<ResponseDto<Void>> deleteQuiz(@PathVariable UUID quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.ok(ResponseDto.success(null, "Xóa quiz thành công"));
    }

    @PutMapping("/{quizId}/publish")
    @Operation(
            summary = "Chuyển trạng thái publish/unpublish của quiz",
            description = """
                Toggle trạng thái publish của quiz.
                - `isPublished = false` → sau khi gọi sẽ thành `true` (đã publish)
                - `isPublished = true` → sau khi gọi sẽ thành `false` (unpublish)
                
                **Điều kiện publish:** Quiz phải có ít nhất 1 câu hỏi.
                
                **Lỗi có thể xảy ra:**
                - `400` — Quiz chưa có câu hỏi nào
                - `404` — Không tìm thấy quiz
                """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> togglePublish(@PathVariable UUID quizId) {
        return ResponseEntity.ok(
                ResponseDto.success(quizService.togglePublish(quizId), "Cập nhật trạng thái publish thành công"));
    }

    @PostMapping("/{quizId}/questions")
    @Operation(
            summary = "Thêm danh sách câu hỏi vào quiz",
            description = """
                Thêm một hoặc nhiều câu hỏi mới vào quiz đã tồn tại.
                Quiz không được ở trạng thái published.
                
                - `questionOrder` phải chưa tồn tại trong quiz.
                - Mỗi câu hỏi phải có ít nhất 1 đáp án đúng.
                
                **Lỗi có thể xảy ra:**
                - `400` — Thứ tự câu hỏi bị trùng, quiz đang published
                - `404` — Không tìm thấy quiz
                """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> addQuestions(
            @PathVariable UUID quizId,
            @Valid @RequestBody List<@Valid QuizQuestionRequest> questions) {
        return ResponseEntity.ok(
                ResponseDto.success(quizService.addQuestions(quizId, questions), "Thêm câu hỏi thành công"));
    }

    @PutMapping("/{quizId}/questions/{questionId}")
    @Operation(
            summary = "Cập nhật một câu hỏi",
            description = """
                Cập nhật nội dung và đáp án cho một câu hỏi cụ thể.
                Toàn bộ đáp án cũ sẽ bị xóa và thay thế bằng đáp án mới.
                
                **Lỗi có thể xảy ra:**
                - `400` — Thứ tự câu hỏi bị trùng, thiếu đáp án đúng
                - `404` — Không tìm thấy câu hỏi trong quiz này
                """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> updateQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuizQuestionRequest request) {
        return ResponseEntity.ok(
                ResponseDto.success(
                        quizService.updateQuestion(quizId, questionId, request),
                        "Cập nhật câu hỏi thành công"));
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    @Operation(
            summary = "Xóa một câu hỏi khỏi quiz",
            description = """
                    Xóa một câu hỏi và tất cả đáp án của nó khỏi quiz.
                    
                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy câu hỏi trong quiz này
                    """
    )
    public ResponseEntity<ResponseDto<Void>> deleteQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        quizService.deleteQuestion(quizId, questionId);
        return ResponseEntity.ok(ResponseDto.success(null, "Xóa câu hỏi thành công"));
    }
}
