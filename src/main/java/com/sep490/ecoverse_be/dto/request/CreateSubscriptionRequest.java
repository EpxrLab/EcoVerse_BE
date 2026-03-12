package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSubscriptionRequest(

        @NotNull(message = "Plan ID is required")
        UUID planId
) {
}
