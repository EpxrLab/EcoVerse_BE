package com.sep490.ecoverse_be.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizExcelRowDto {

    private int rowNumber;
    private String quizTitle;
    private String description;
    private String difficulty;
    private String targetGrade;
    private String quizType;
    private String questionOrder;
    private String questionText;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    private String correctAnswer;
    private String coinsOnPass;
    private String timePerQuestion;
    private String passScorePercentage;
}
