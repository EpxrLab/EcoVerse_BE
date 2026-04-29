package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitQuizAttemptRequest {

    @Valid
    private List<QuizAttemptAnswerSubmit> answers;
}
