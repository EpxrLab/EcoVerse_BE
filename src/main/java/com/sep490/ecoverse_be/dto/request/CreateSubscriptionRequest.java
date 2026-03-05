package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionRequest(

        @NotNull(message = "Plan ID is required")
        Long planId
) {
}
