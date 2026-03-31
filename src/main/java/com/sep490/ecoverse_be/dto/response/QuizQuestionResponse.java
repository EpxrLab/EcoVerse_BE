package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizQuestionResponse {

    private UUID id;
    private int questionOrder;
    private String questionText;
    private String questionImageUrl;
    private String questionImagePresignedUrl;
    private List<QuizAnswerResponse> answers;
}
