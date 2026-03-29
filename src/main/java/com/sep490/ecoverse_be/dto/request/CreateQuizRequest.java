package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.QuizType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateQuizRequest {

    @NotBlank(message = "Tiêu đề quiz không được rỗng")
    @Size(max = 255, message = "Tiêu đề quiz tối đa 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Độ khó không được rỗng")
    private QuizDifficulty difficulty;

    @NotNull(message = "Loại quiz không được rỗng")
    private QuizType quizType;

    @Min(value = 1, message = "Khối lớp phải >= 1")
    @Max(value = 5, message = "Khối lớp phải <= 5")
    private Integer targetGrade;

    @Builder.Default
    @Min(value = 1, message = "Điểm thưởng phải >= 1")
    private Integer coinOnPass = 10;

    @Min(value = 5, message = "Thời gian mỗi câu phải >= 5 giây")
    private Integer timePerQuestion;

    @Builder.Default
    @Min(value = 1, message = "Tỉ lệ đạt phải >= 1%")
    @Max(value = 100, message = "Tỉ lệ đạt phải <= 100%")
    private Integer passScorePercentage = 70;

    @NotEmpty(message = "Quiz phải có ít nhất một câu hỏi")
    @Valid
    private List<QuizQuestionRequest> questions;
}
