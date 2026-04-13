package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.entity.FileEntity;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval Augmented Generation) service.
 *
 * Pipeline:
 * 1. Download files từ S3
 * 2. Extract text (PDF via PDFBox, DOCX via Apache POI, TXT trực tiếp)
 * 3. Chunk text thành đoạn nhỏ (500 chars, 100 overlap)
 * 4. Embed tất cả chunks bằng Gemini Embedding API
 * 5. Build query từ campaign + waste items context
 * 6. Embed query
 * 7. Cosine similarity → lấy top-K chunks phù hợp nhất
 * 8. Trả về relevant content cho prompt
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentRagService {

    private final S3Client s3Client;
    private final FileRepository fileRepository;
    private final GeminiService geminiService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;
    private static final int TOP_K = 10;
    private static final int SMALL_DOC_THRESHOLD = 3000; // chars — dưới ngưỡng thì dùng full text

    /**
     * Main entry: lấy nội dung liên quan từ danh sách file IDs.
     *
     * @param fileIds   danh sách file IDs đã upload
     * @param query     query string (campaign name + waste items) để tìm đoạn liên quan
     * @return          concatenated relevant text, sẵn sàng đưa vào prompt
     */
    public String getRelevantContent(List<UUID> fileIds, String query) {
        if (fileIds == null || fileIds.isEmpty()) {
            return "";
        }

        // Tổng hợp text từ tất cả files
        StringBuilder allText = new StringBuilder();
        for (UUID fileId : fileIds) {
            String text = extractTextFromFile(fileId);
            if (!text.isBlank()) {
                allText.append(text).append("\n\n");
            }
        }

        String fullText = allText.toString().trim();
        if (fullText.isEmpty()) {
            return "";
        }

        // Nếu tổng text đủ nhỏ → dùng toàn bộ, không cần RAG
        if (fullText.length() <= SMALL_DOC_THRESHOLD) {
            log.info("Document text nhỏ ({}), dùng full text không RAG", fullText.length());
            return fullText;
        }

        // RAG pipeline: chunk → embed → retrieve
        log.info("Document text lớn ({}), chạy RAG pipeline", fullText.length());
        return retrieveRelevantChunks(fullText, query);
    }

    // ── Step 1: Download + Extract text ──────────────────────────────────────

    private String extractTextFromFile(UUID fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElse(null);
        if (fileEntity == null) {
            log.warn("File ID {} không tồn tại, bỏ qua", fileId);
            return "";
        }

        try {
            byte[] fileBytes = downloadFromS3(fileEntity.getFileUrl());
            String fileType = fileEntity.getFileType() != null
                    ? fileEntity.getFileType().toLowerCase()
                    : detectFileType(fileEntity.getFileName());

            return extractText(fileBytes, fileType, fileEntity.getFileName());

        } catch (Exception e) {
            log.warn("Lỗi extract text từ file {}: {}", fileEntity.getFileName(), e.getMessage());
            return "";
        }
    }

    private byte[] downloadFromS3(String keyOrUrl) {
        String key = extractS3Key(keyOrUrl);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(getRequest);
        return response.asByteArray();
    }

    private String extractText(byte[] fileBytes, String fileType, String fileName) {
        try {
            if (fileType.contains("pdf")) {
                return extractTextFromPdf(fileBytes);
            } else if (fileType.contains("wordprocessingml") || fileType.contains("docx")
                    || fileName.toLowerCase().endsWith(".docx")) {
                return extractTextFromDocx(fileBytes);
            } else if (fileType.contains("text") || fileName.toLowerCase().endsWith(".txt")) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            } else {
                log.info("Unsupported file type '{}' cho RAG, bỏ qua: {}", fileType, fileName);
                return "";
            }
        } catch (Exception e) {
            log.warn("Lỗi đọc nội dung file {}: {}", fileName, e.getMessage());
            return "";
        }
    }

    private String extractTextFromPdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    private String extractTextFromDocx(byte[] fileBytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.isBlank()) {
                    text.append(paraText).append("\n");
                }
            }
            return text.toString().trim();
        }
    }

    // ── Step 2: Chunking ─────────────────────────────────────────────────────

    /**
     * Chia text thành các chunks nhỏ với overlap.
     * Cố gắng cắt ở ranh giới câu để giữ ngữ cảnh.
     */
    List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        int length = text.length();
        int start = 0;

        while (start < length) {
            int prevStart = start;
            int end = Math.min(start + CHUNK_SIZE, length);

            // Cố gắng cắt ở ranh giới câu (dấu chấm, dấu xuống dòng)
            if (end < length) {
                int lastPeriod = text.lastIndexOf('.', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);

                if (breakPoint > start + CHUNK_SIZE / 2) {
                    end = breakPoint + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end - CHUNK_OVERLAP;
            if (start <= prevStart) {
                start = end; // Fallback to avoid infinite loop
            }
            if (end >= length) {
                break;
            }
            if (start < 0) start = 0;
            if (start >= end) start = end; // prevent infinite loop
        }

        return chunks;
    }

    // ── Step 3: Embed + Retrieve ─────────────────────────────────────────────

    private String retrieveRelevantChunks(String fullText, String query) {
        List<String> chunks = chunkText(fullText);

        if (chunks.isEmpty()) {
            return fullText.length() > SMALL_DOC_THRESHOLD * 2
                    ? fullText.substring(0, SMALL_DOC_THRESHOLD * 2)
                    : fullText;
        }

        log.info("RAG: {} chunks tạo từ document, embedding...", chunks.size());

        // Embed tất cả chunks
        List<List<Double>> chunkEmbeddings = geminiService.batchEmbedTexts(chunks);

        if (chunkEmbeddings.isEmpty() || chunkEmbeddings.size() != chunks.size()) {
            log.warn("Embedding thất bại, fallback dùng {} chars đầu tiên", SMALL_DOC_THRESHOLD * 2);
            return fullText.substring(0, Math.min(fullText.length(), SMALL_DOC_THRESHOLD * 2));
        }

        // Embed query
        List<Double> queryEmbedding = geminiService.embedText(query);
        if (queryEmbedding.isEmpty()) {
            log.warn("Query embedding thất bại, fallback dùng first chunks");
            return chunks.stream().limit(TOP_K).collect(Collectors.joining("\n\n"));
        }

        // Tính cosine similarity và lấy top-K
        List<ChunkScore> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double similarity = cosineSimilarity(queryEmbedding, chunkEmbeddings.get(i));
            scored.add(new ChunkScore(i, similarity));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score)); // descending

        StringBuilder result = new StringBuilder();
        int count = Math.min(TOP_K, scored.size());
        for (int i = 0; i < count; i++) {
            result.append(chunks.get(scored.get(i).index)).append("\n\n");
        }

        log.info("RAG: trích xuất {} relevant chunks (top scores: {})",
                count, scored.stream().limit(3)
                        .map(s -> String.format("%.3f", s.score))
                        .collect(Collectors.joining(", ")));

        return result.toString().trim();
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA.size() != vecB.size() || vecA.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            dotProduct += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    private String extractS3Key(String keyOrUrl) {
        if (keyOrUrl.startsWith("https://") || keyOrUrl.startsWith("http://")) {
            int amazonIndex = keyOrUrl.indexOf(".amazonaws.com/");
            if (amazonIndex != -1) {
                return keyOrUrl.substring(amazonIndex + ".amazonaws.com/".length());
            }
        }
        return keyOrUrl;
    }

    private String detectFileType(String fileName) {
        if (fileName == null) return "";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".txt")) return "text/plain";
        return "";
    }

    private record ChunkScore(int index, double score) {}
}
