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

    @Size(max = 255, message = "Tieu de quiz toi da 255 ky tu")
    private String title;

    private String description;

    private QuizDifficulty difficulty;

    private QuizType quizType;

    @Min(value = 1, message = "Diem thuong phai >= 1")
    private Integer pointsReward;

    @Min(value = 5, message = "Thoi gian moi cau phai >= 5 giay")
    private Integer timePerQuestion;

    @Min(value = 1, message = "Ti le dat phai >= 1%")
    @Max(value = 100, message = "Ti le dat phai <= 100%")
    private Integer passScorePercentage;
}
