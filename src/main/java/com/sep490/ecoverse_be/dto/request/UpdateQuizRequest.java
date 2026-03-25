package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.QuizType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateQuizRequest {

    @Size(max = 255, message = "Tiêu đề quiz tối đa 255 ký tự")
    private String title;

    private String description;

    private QuizDifficulty difficulty;

    private QuizType quizType;

    @Min(value = 1, message = "Khối lớp phải >= 1")
    @Max(value = 5, message = "Khối lớp phải <= 5")
    private Integer targetGrade;

    @Min(value = 1, message = "Điểm thưởng phải >= 1")
    private Integer pointsReward;

    @Min(value = 5, message = "Thời gian mỗi câu phải >= 5 giây")
    private Integer timePerQuestion;

    @Min(value = 1, message = "Tỉ lệ đạt phải >= 1%")
    @Max(value = 100, message = "Tỉ lệ đạt phải <= 100%")
    private Integer passScorePercentage;
}
