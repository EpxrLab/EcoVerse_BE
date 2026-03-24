package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AssignStudentsRequest {

    @NotEmpty
    private List<UUID> studentIds;
}

