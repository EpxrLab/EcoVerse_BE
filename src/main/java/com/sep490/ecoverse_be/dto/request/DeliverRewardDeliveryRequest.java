package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliverRewardDeliveryRequest {

    @NotBlank(message = "Anh bang chung giao qua khong duoc de trong")
    private String deliveryImageUrl;

    private String notes;
}
