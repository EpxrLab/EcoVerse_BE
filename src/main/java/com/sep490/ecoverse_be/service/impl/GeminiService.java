package com.sep490.ecoverse_be.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.ecoverse_be.exception.BadRequestException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service đóng gói các lời gọi đến Gemini REST API.
 * - generateContent: tạo nội dung quiz từ prompt
 * - embedContent / batchEmbedContents: tạo embedding vector cho RAG
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.embedding-model}")
    private String embeddingModel;

    // ── DTOs cho Gemini request/response ──────────────────────────────────────

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeminiQuizQuestion {
        private String questionText;
        private List<GeminiQuizAnswer> answers;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeminiQuizAnswer {
        private String answerText;
        private boolean correct;
    }

    // ── Generate content ─────────────────────────────────────────────────────

    /**
     * Gọi Gemini generateContent API với system instruction và user prompt.
     * Trả về danh sách câu hỏi đã parse từ JSON response.
     */
    public List<GeminiQuizQuestion> generateQuizQuestions(String systemPrompt, String userPrompt) {
        String url = String.format("%s/%s:generateContent?key=%s", BASE_URL, model, apiKey);

        // Build request body
        Map<String, Object> body = new LinkedHashMap<>();

        // System instruction
        body.put("system_instruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        ));

        // User content
        body.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", userPrompt)))
        ));

        // Generation config — force JSON output
        body.put("generationConfig", Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.7,
                "topP", 0.9
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        long startMs = System.currentTimeMillis();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            long durationMs = System.currentTimeMillis() - startMs;
            log.info("Gemini generateContent completed in {}ms", durationMs);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BadRequestException("Gemini API trả về lỗi: " + response.getStatusCode());
            }

            return parseGenerateResponse(response.getBody());

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi gọi Gemini generateContent", e);
            throw new BadRequestException("Lỗi gọi Gemini AI: " + e.getMessage());
        }
    }

    /**
     * Parse Gemini generateContent response.
     * Response format: { candidates: [{ content: { parts: [{ text: "JSON string" }] } }] }
     */
    private List<GeminiQuizQuestion> parseGenerateResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty() || candidates.isMissingNode()) {
                throw new BadRequestException("Gemini không trả về kết quả nào");
            }

            String text = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            if (text == null || text.isBlank()) {
                throw new BadRequestException("Gemini trả về nội dung rỗng");
            }

            // Parse JSON text thành danh sách câu hỏi
            List<GeminiQuizQuestion> questions = objectMapper.readValue(
                    text, new TypeReference<List<GeminiQuizQuestion>>() {}
            );

            if (questions == null || questions.isEmpty()) {
                throw new BadRequestException("Gemini không tạo được câu hỏi nào");
            }

            return questions;

        } catch (JsonProcessingException e) {
            log.error("Lỗi parse Gemini response JSON", e);
            throw new BadRequestException("Lỗi phân tích kết quả từ Gemini AI: " + e.getMessage());
        }
    }

    // ── Embedding ────────────────────────────────────────────────────────────

    /**
     * Tạo embedding vector cho 1 đoạn text.
     */
    public List<Double> embedText(String text) {
        String url = String.format("%s/%s:embedContent?key=%s", BASE_URL, embeddingModel, apiKey);

        Map<String, Object> body = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of("parts", List.of(Map.of("text", truncateForEmbedding(text))))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Gemini embedding thất bại: {}", response.getStatusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("embedding").path("values");

            if (values.isMissingNode() || !values.isArray()) {
                return Collections.emptyList();
            }

            List<Double> embedding = new ArrayList<>();
            for (JsonNode val : values) {
                embedding.add(val.asDouble());
            }
            return embedding;

        } catch (Exception e) {
            log.warn("Lỗi gọi Gemini embedding, bỏ qua RAG cho đoạn này", e);
            return Collections.emptyList();
        }
    }

    /**
     * Batch embed nhiều đoạn text cùng lúc.
     */
    public List<List<Double>> batchEmbedTexts(List<String> texts) {
        String url = String.format("%s/%s:batchEmbedContents?key=%s", BASE_URL, embeddingModel, apiKey);

        List<Map<String, Object>> requests = new ArrayList<>();
        for (String text : texts) {
            requests.add(Map.of(
                    "model", "models/" + embeddingModel,
                    "content", Map.of("parts", List.of(Map.of("text", truncateForEmbedding(text))))
            ));
        }

        Map<String, Object> body = Map.of("requests", requests);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Gemini batch embedding thất bại: {}", response.getStatusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddings = root.path("embeddings");

            List<List<Double>> result = new ArrayList<>();
            for (JsonNode embeddingNode : embeddings) {
                JsonNode values = embeddingNode.path("values");
                List<Double> vec = new ArrayList<>();
                for (JsonNode val : values) {
                    vec.add(val.asDouble());
                }
                result.add(vec);
            }
            return result;

        } catch (Exception e) {
            log.warn("Lỗi gọi Gemini batch embedding", e);
            return Collections.emptyList();
        }
    }

    /**
     * Giới hạn text cho embedding (Gemini embedding có giới hạn token).
     */
    private String truncateForEmbedding(String text) {
        int maxChars = 8000; // ~2000 tokens, safe limit
        if (text.length() > maxChars) {
            return text.substring(0, maxChars);
        }
        return text;
    }
}
