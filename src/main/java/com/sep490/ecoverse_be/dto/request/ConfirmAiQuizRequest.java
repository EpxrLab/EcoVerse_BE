package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmAiQuizRequest {

    @NotNull(message = "AI Generation Log ID không được rỗng")
    private UUID aiGenerationLogId;

    /**
     * Cho phép user chỉnh sửa quiz trước khi lưu.
     * Toàn bộ dữ liệu quiz (đã chỉnh sửa hoặc giữ nguyên từ preview) được gửi lại.
     */
    @NotEmpty(message = "Danh sách câu hỏi không được rỗng")
    @Valid
    private List<QuizQuestionRequest> questions;
}
