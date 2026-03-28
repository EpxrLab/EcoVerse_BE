package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class QuizAttemptAnswerSubmit {

    @NotNull(message = "questionId khong duoc null")
    private UUID questionId;

    // selectedAnswerId co the null neu student bo trong
    private UUID selectedAnswerId;
}
