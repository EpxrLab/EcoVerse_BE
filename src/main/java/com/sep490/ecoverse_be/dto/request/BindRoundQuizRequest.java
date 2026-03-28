package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BindRoundQuizRequest {

    @NotNull(message = "quizId không được null")
    private UUID quizId;

    // So lan lam lai toi da (mac dinh 3, configurable 1-10)
    @Min(value = 1, message = "Số lần làm tối thiểu là 1")
    @Max(value = 10, message = "Số lần làm tối đa là 10")
    private Integer maxAttempts = 3;

    // Thu tu hien thi trong round (mac dinh 1)
    @Min(value = 1, message = "Thứ tự hiển thị phải >= 1")
    private Integer displayOrder = 1;

    // Bat buoc hoan thanh de xep hang (mac dinh true)
    private Boolean isRequired = true;
}
