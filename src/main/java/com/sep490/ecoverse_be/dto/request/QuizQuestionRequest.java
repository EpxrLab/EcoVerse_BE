package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizQuestionRequest {

    @Min(value = 1, message = "Thứ tự câu hỏi phải >= 1")
    private int questionOrder;

    @NotBlank(message = "Nội dung câu hỏi không được rỗng")
    private String questionText;

    @NotEmpty(message = "Câu hỏi phải có ít nhất một đáp án")
    @Size(min = 2, max = 4, message = "Câu hỏi phải có từ 2 đến 4 đáp án")
    @Valid
    private List<QuizAnswerRequest> answers;
}
