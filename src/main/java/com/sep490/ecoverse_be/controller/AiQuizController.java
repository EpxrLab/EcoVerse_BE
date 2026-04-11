package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.ConfirmAiQuizRequest;
import com.sep490.ecoverse_be.dto.request.GenerateAiQuizRequest;
import com.sep490.ecoverse_be.dto.response.AiQuizPreviewResponse;
import com.sep490.ecoverse_be.dto.response.QuizResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.IAiQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz/ai")
@PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP')")
@Tag(name = "AI Quiz Generation", description = "APIs tạo quiz tự động bằng AI (Gemini) với RAG")
public class AiQuizController {

    @Autowired
    private IAiQuizService aiQuizService;

    @PostMapping("/generate")
    @Operation(
            summary = "Tạo quiz AI (preview, chưa lưu DB)",
            description = """
                    Gọi Gemini AI để tạo quiz dựa trên:
                    - Nội dung chiến dịch (campaign)
                    - Waste items từ round game config
                    - File tài liệu đã upload (optional, xử lý bằng RAG)
                    
                    **Quy trình:**
                    1. Validate quota AI usage trong kỳ subscription
                    2. Lấy thông tin campaign + waste items + file content (RAG)
                    3. Gọi Gemini AI tạo câu hỏi
                    4. Trừ 1 AI usage ngay khi thành công
                    5. Trả về kết quả preview (chưa lưu DB)
                    
                    **Giới hạn:**
                    - Số câu hỏi: 15–30
                    - Lớp: 1–12
                    - File hỗ trợ: PDF, DOCX, TXT
                    
                    **Response:** cùng format với QuizResponse + aiGenerationLogId
                    
                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<AiQuizPreviewResponse>> generateAiQuiz(
            @Valid @RequestBody GenerateAiQuizRequest request) {
        AiQuizPreviewResponse response = aiQuizService.generateAiQuiz(request);
        return ResponseEntity.ok(
                ResponseDto.success(response, "Tạo quiz AI thành công. Vui lòng xem trước và xác nhận lưu."));
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Xác nhận lưu quiz AI vào database",
            description = """
                    Sau khi preview quiz AI, School/Partnership xác nhận lưu vào database.
                    
                    **Cho phép chỉnh sửa:** Frontend có thể gửi lại danh sách câu hỏi 
                    đã được chỉnh sửa trước khi lưu.
                    
                    **Request body:**
                    - `aiGenerationLogId`: ID từ response của endpoint generate
                    - `questions`: danh sách câu hỏi (giữ nguyên hoặc đã chỉnh sửa)
                    
                    **Response:** QuizResponse đầy đủ (quiz đã lưu, isPublished = false)
                    
                    **Lỗi có thể xảy ra:**
                    - `404` — Không tìm thấy AI Generation Log
                    - `400` — Quiz đã được lưu trước đó / Không có quyền
                    
                    **Header:**
                    ```
                    Authorization: Bearer <accessToken>
                    Content-Type: application/json
                    ```
                    """
    )
    public ResponseEntity<ResponseDto<QuizResponse>> confirmAiQuiz(
            @Valid @RequestBody ConfirmAiQuizRequest request) {
        QuizResponse response = aiQuizService.confirmAiQuiz(request);
        return ResponseEntity.status(201).body(
                ResponseDto.created(response, "Lưu quiz AI thành công"));
    }
}
