package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ExtendInvitingRequest {

    @NotNull
    private LocalDateTime newInviteEndAt;

    private List<UUID> additionalStudentIds;

    private String reason;
}

