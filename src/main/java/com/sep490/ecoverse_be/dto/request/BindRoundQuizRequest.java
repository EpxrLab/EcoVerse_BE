package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BindRoundQuizRequest {

    /**
     * Danh sách quiz (1 hoặc nhiều) cùng chung {@link #maxAttempts} và {@link #isRequired}.
     * Ví dụ [A,B,C] + maxAttempts 2 → cả 3 quiz đều maxAttempts = 2. Một quiz: {@code quizIds: ["uuid"]}.
     */
    private List<UUID> quizIds;

    @Min(value = 1, message = "Số lần làm tối thiểu là 1")
    @Max(value = 10, message = "Số lần làm tối đa là 10")
    private Integer maxAttempts;

    private Boolean isRequired;
}
