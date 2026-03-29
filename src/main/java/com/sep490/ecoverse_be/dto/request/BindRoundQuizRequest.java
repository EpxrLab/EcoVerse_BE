package com.sep490.ecoverse_be.dto.request;

<<<<<<< HEAD
=======
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
>>>>>>> 67bfed110da3976363bf57f68c3bcbf51382cb0c
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BindRoundQuizRequest {

    private UUID quizId;

    private List<UUID> quizIds;

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
