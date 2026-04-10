package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.UUID;

/**
 * Wrapper cho preview kết quả quiz AI.
 * quizPreview có cùng cấu trúc QuizResponse (id sẽ null vì chưa lưu DB).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiQuizPreviewResponse {

    /**
     * ID của AiGenerationLog — dùng cho endpoint confirm để lưu quiz.
     */
    private UUID aiGenerationLogId;

    /**
     * Dữ liệu quiz preview đầy đủ, cùng format với QuizResponse.
     */
    private QuizResponse quizPreview;
}
