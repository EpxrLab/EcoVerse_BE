package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RenewSubscriptionRequest(

        @NotNull(message = "Subscription ID is required")
        UUID subscriptionId,

        UUID planId
) {
}
