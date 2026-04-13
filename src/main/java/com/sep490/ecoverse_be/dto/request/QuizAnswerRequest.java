package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizAnswerRequest {

    @NotBlank(message = "Nội dung đáp án không được rỗng")
    private String answerText;

    private boolean correct;
}
