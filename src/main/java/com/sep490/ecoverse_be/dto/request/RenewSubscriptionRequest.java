package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;

public record RenewSubscriptionRequest(

        @NotNull(message = "Subscription ID is required")
        Long subscriptionId,

        Long planId
) {
}
