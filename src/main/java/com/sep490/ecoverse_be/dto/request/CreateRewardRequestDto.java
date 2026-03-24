package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateRewardRequestDto {

    @NotNull(message = "rewardId khong duoc rong")
    private UUID rewardId;

    @Min(value = 1, message = "So luong phai >= 1")
    private int quantity = 1;

    private String notes;

    // Chi duoc dien khi parent tao thay cho con
    private UUID studentId;
}
