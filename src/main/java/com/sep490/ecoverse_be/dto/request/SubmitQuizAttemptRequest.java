package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitQuizAttemptRequest {

    @NotEmpty(message = "Danh sach dap an khong duoc rong")
    @Valid
    private List<QuizAttemptAnswerSubmit> answers;
}
