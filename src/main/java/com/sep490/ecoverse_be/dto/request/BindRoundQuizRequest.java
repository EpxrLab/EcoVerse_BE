package com.sep490.ecoverse_be.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BindRoundQuizRequest {

    private UUID quizId;

    private List<UUID> quizIds;
}

