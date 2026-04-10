package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.SubmitGameSessionRequest;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionResultResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionStartResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface IStudentGameService {

    StudentGameSessionStartResponse startGameSession(UUID campaignId, UUID roundId, UUID roundGameConfigId, Integer levelNumber);

    StudentGameSessionResultResponse submitGameSession(UUID sessionId, SubmitGameSessionRequest request);

    StudentGameSessionResultResponse getGameSessionResult(UUID sessionId);

    List<StudentGameSessionSummaryResponse> getGameSessionHistory(UUID campaignId, UUID roundId, UUID roundGameConfigId);

    List<StudentGameSessionSummaryResponse> getOpenSessionsByStudentId(UUID studentId);
}
