package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.ConfirmAiQuizRequest;
import com.sep490.ecoverse_be.dto.request.GenerateAiQuizRequest;
import com.sep490.ecoverse_be.dto.response.AiQuizPreviewResponse;
import com.sep490.ecoverse_be.dto.response.QuizResponse;

public interface IAiQuizService {

    /**
     * Gọi Gemini AI để tạo quiz dựa trên campaign, waste items, và file import (nếu có).
     * Trừ 1 AI usage ngay khi thành công. Trả về preview (chưa lưu DB).
     */
    AiQuizPreviewResponse generateAiQuiz(GenerateAiQuizRequest request);

    /**
     * Xác nhận lưu quiz AI đã preview vào database.
     */
    QuizResponse confirmAiQuiz(ConfirmAiQuizRequest request);
}
