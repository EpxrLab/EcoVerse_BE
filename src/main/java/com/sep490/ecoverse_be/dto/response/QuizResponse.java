package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizCreated;
import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.QuizSource;
import com.sep490.ecoverse_be.enums.QuizType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizResponse {

    private UUID id;
    private String title;
    private String description;
    private QuizDifficulty difficulty;
    private QuizType quizType;
    private QuizSource source;
    private QuizCreated createdBy;
    private Integer targetGrade;
    private Integer coinsOnPass;
    private Integer timePerQuestion;
    private Integer passScorePercentage;
    private boolean isPublished;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QuizQuestionResponse> questions;
}
