package com.sep490.ecoverse_be.event;

import com.sep490.ecoverse_be.entity.FileEntity;
import com.sep490.ecoverse_be.enums.EmbeddingStatus;
import com.sep490.ecoverse_be.repository.FileRepository;
import com.sep490.ecoverse_be.service.impl.DocumentRagService;
import com.sep490.ecoverse_be.service.impl.GeminiService;
import com.sep490.ecoverse_be.service.impl.QdrantVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Async listener xử lý sự kiện upload tài liệu.
 * <p>
 * Pipeline:
 * 1. Nhận DocumentUploadedEvent
 * 2. Cập nhật trạng thái → PROCESSING
 * 3. Extract text từ file (PDF/DOCX/TXT) qua S3
 * 4. Chunk text
 * 5. Embed chunks bằng Gemini API
 * 6. Upsert vectors vào Qdrant
 * 7. Cập nhật trạng thái → COMPLETED (hoặc FAILED)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentEmbeddingListener {

    private final FileRepository fileRepository;
    private final DocumentRagService documentRagService;
    private final GeminiService geminiService;
    private final QdrantVectorStoreService qdrantVectorStoreService;

    @Async
    @EventListener
    @Transactional
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        UUID fileId = event.getFileId();
        log.info("Bắt đầu auto-embed document: fileId={}", fileId);

        FileEntity fileEntity = fileRepository.findById(fileId).orElse(null);
        if (fileEntity == null) {
            log.warn("File {} không tồn tại, bỏ qua embedding", fileId);
            return;
        }

        try {
            // Cập nhật trạng thái → PROCESSING
            fileEntity.setEmbeddingStatus(EmbeddingStatus.PROCESSING);
            fileRepository.save(fileEntity);

            // 1. Extract text từ file
            String fullText = documentRagService.extractTextFromFilePublic(fileId);
            if (fullText == null || fullText.isBlank()) {
                log.warn("File {} không có text content, đánh dấu COMPLETED (empty)", fileId);
                fileEntity.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
                fileRepository.save(fileEntity);
                return;
            }

            // 2. Chunk text
            List<String> chunks = documentRagService.chunkText(fullText);
            if (chunks.isEmpty()) {
                log.warn("File {} không tạo được chunks nào, đánh dấu COMPLETED", fileId);
                fileEntity.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
                fileRepository.save(fileEntity);
                return;
            }

            log.info("File {}: extracted {} chars, {} chunks", fileId, fullText.length(), chunks.size());

            // 3. Embed chunks bằng Gemini
            List<List<Double>> embeddings = geminiService.batchEmbedTexts(chunks);
            if (embeddings.isEmpty() || embeddings.size() != chunks.size()) {
                log.error("File {}: Embedding thất bại (got {} embeddings for {} chunks)",
                        fileId, embeddings.size(), chunks.size());
                fileEntity.setEmbeddingStatus(EmbeddingStatus.FAILED);
                fileRepository.save(fileEntity);
                return;
            }

            // 4. Upsert vào Qdrant
            qdrantVectorStoreService.upsertChunks(fileId, chunks, embeddings);

            // 5. Cập nhật trạng thái → COMPLETED
            fileEntity.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
            fileRepository.save(fileEntity);

            log.info("Auto-embed thành công cho file {}: {} chunks đã lưu vào Qdrant",
                    fileId, chunks.size());

        } catch (Exception e) {
            log.error("Auto-embed thất bại cho file {}: {}", fileId, e.getMessage(), e);
            try {
                fileEntity.setEmbeddingStatus(EmbeddingStatus.FAILED);
                fileRepository.save(fileEntity);
            } catch (Exception saveError) {
                log.error("Không thể cập nhật trạng thái FAILED cho file {}", fileId, saveError);
            }
        }
    }
}
