package com.sep490.ecoverse_be.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipRewardDeliveryRequest {

    private String trackingCode;
    private String notes;
}
