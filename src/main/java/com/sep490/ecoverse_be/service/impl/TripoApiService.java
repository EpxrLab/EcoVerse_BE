package com.sep490.ecoverse_be.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class TripoApiService {

    private final RestTemplate restTemplate;

    @Value("${tripo.api-key:}")
    private String apiKey;

    private static final String BASE_URL = "https://api.3daistudio.com/v1";

    public TripoApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String submitImageTo3dTask(String imageUrl) {
        String url = BASE_URL + "/3d-models/tripo/image-to-3d/";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("Content-Type", "application/json");

        TripoImageTo3DRequest body = TripoImageTo3DRequest.builder()
                .imageUrl(imageUrl)
                .texture(true)
                .pbr(true)
                .textureAlignment("original_image")
                .build();

        HttpEntity<TripoImageTo3DRequest> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TripoGenerationResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, TripoGenerationResponse.class);
            if (response.getBody() == null || response.getBody().getTaskId() == null) {
                throw new FuncErrorException("Failed to start Tripo task. No task_id returned.");
            }
            return response.getBody().getTaskId();
        } catch (Exception e) {
            log.error("Error submitting Tripo image-to-3d task", e);
            throw new FuncErrorException("Tripo API Error: " + e.getMessage());
        }
    }

    public TripoTaskStatusResponse checkTaskStatus(String taskId) {
        String url = BASE_URL + "/generation-request/" + taskId + "/status/";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TripoTaskStatusResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, TripoTaskStatusResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error checking Tripo task status for {}", taskId, e);
            throw new FuncErrorException("Tripo API Error: " + e.getMessage());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripoImageTo3DRequest {
        @JsonProperty("image_url")
        private String imageUrl;
        private boolean texture;
        private boolean pbr;
        @JsonProperty("texture_alignment")
        private String textureAlignment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripoGenerationResponse {
        @JsonProperty("task_id")
        private String taskId;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripoTaskStatusResponse {
        private String status; // PENDING, FINISHED, FAILED
        private int progress;
        @JsonProperty("failure_reason")
        private String failureReason;
        private List<TripoResult> results;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripoResult {
        private String asset;
        @JsonProperty("asset_type")
        private String assetType;
        private Object metadata;
    }
}
