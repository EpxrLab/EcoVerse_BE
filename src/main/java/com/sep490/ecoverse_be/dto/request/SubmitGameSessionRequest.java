package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitGameSessionRequest {

    @NotNull(message = "totalItems không được để trống")
    @Min(value = 0, message = "totalItems phải >= 0")
    private Integer totalItems;

    @NotNull(message = "correctItems không được để trống")
    @Min(value = 0, message = "correctItems phải >= 0")
    private Integer correctItems;

    @NotNull(message = "incorrectItems không được để trống")
    @Min(value = 0, message = "incorrectItems phải >= 0")
    private Integer incorrectItems;

    @Min(value = 0, message = "timeTakenSeconds phải >= 0")
    private Integer timeTakenSeconds;
}
