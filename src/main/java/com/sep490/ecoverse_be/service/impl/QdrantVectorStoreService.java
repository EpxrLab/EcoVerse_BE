package com.sep490.ecoverse_be.service.impl;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Service quản lý vector embeddings trong Qdrant.
 * <p>
 * Responsibilities:
 * - Tự động tạo collection khi khởi động nếu chưa tồn tại
 * - Upsert document chunks (text + embedding) theo fileId
 * - Search similar chunks bằng cosine similarity + filter theo fileIds
 * - Xóa tất cả chunks của một file khi file bị xóa
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QdrantVectorStoreService {

    private final QdrantClient qdrantClient;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    /**
     * Dimension của Gemini embedding-2-preview.
     * Model mặc định trả về 3072-dim vectors.
     */
    private static final int VECTOR_DIMENSION = 3072;

    // ── Init ─────────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        ensureCollection();
    }

    /**
     * Tạo collection nếu chưa tồn tại.
     */
    private void ensureCollection() {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!exists) {
                qdrantClient.createCollectionAsync(collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(VECTOR_DIMENSION)
                                .build()
                ).get();
                log.info("Qdrant collection '{}' đã được tạo (dim={}, distance=Cosine)",
                        collectionName, VECTOR_DIMENSION);

                // Tạo payload index cho fileId để filter nhanh hơn
                qdrantClient.createPayloadIndexAsync(
                        collectionName, "fileId",
                        io.qdrant.client.grpc.Collections.PayloadSchemaType.Keyword,
                        null, null, null, null
                ).get();
                log.info("Payload index 'fileId' đã được tạo cho collection '{}'", collectionName);
            } else {
                log.info("Qdrant collection '{}' đã tồn tại, bỏ qua tạo mới", collectionName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Bị interrupted khi kiểm tra/tạo Qdrant collection", e);
        } catch (ExecutionException e) {
            log.error("Lỗi kiểm tra/tạo Qdrant collection: {}", e.getMessage(), e);
        }
    }

    // ── Upsert ───────────────────────────────────────────────────────────────

    /**
     * Lưu các chunks đã embed vào Qdrant.
     *
     * @param fileId     UUID của file entity
     * @param chunks     danh sách text chunks
     * @param embeddings danh sách embedding vectors tương ứng
     */
    public void upsertChunks(UUID fileId, List<String> chunks, List<List<Double>> embeddings) {
        if (chunks.size() != embeddings.size()) {
            log.error("Mismatch chunks ({}) vs embeddings ({})", chunks.size(), embeddings.size());
            return;
        }

        List<PointStruct> points = new ArrayList<>();
        String fileIdStr = fileId.toString();

        for (int i = 0; i < chunks.size(); i++) {
            // Point ID = deterministic UUID from fileId + chunkIndex
            UUID pointId = UUID.nameUUIDFromBytes((fileIdStr + "_chunk_" + i).getBytes());

            // Convert List<Double> → List<Float>
            List<Float> floatVec = embeddings.get(i).stream()
                    .map(Double::floatValue)
                    .toList();

            PointStruct point = PointStruct.newBuilder()
                    .setId(id(pointId))
                    .setVectors(vectors(floatVec))
                    .putAllPayload(Map.of(
                            "fileId", value(fileIdStr),
                            "chunkIndex", value(i),
                            "chunkText", value(chunks.get(i))
                    ))
                    .build();

            points.add(point);
        }

        try {
            // Upsert in batches of 100
            int batchSize = 100;
            for (int start = 0; start < points.size(); start += batchSize) {
                int end = Math.min(start + batchSize, points.size());
                List<PointStruct> batch = points.subList(start, end);
                qdrantClient.upsertAsync(collectionName, batch).get();
            }

            log.info("Qdrant: upserted {} chunks cho file {}", chunks.size(), fileId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant upsert bị interrupted", e);
        } catch (ExecutionException e) {
            log.error("Lỗi upsert vào Qdrant cho file {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Lỗi upsert vào Qdrant", e);
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    /**
     * Tìm kiếm các chunks tương tự trong Qdrant.
     *
     * @param fileIds         danh sách file IDs để filter
     * @param queryEmbedding  embedding vector của query
     * @param topK            số kết quả tối đa
     * @return danh sách text chunks sắp xếp theo độ tương đồng giảm dần
     */
    public List<String> searchSimilarChunks(List<UUID> fileIds, List<Double> queryEmbedding, int topK) {
        try {
            // Convert query embedding to float
            List<Float> floatQuery = queryEmbedding.stream()
                    .map(Double::floatValue)
                    .toList();

            // Build filter: fileId IN [...]
            Filter.Builder filterBuilder = Filter.newBuilder();
            for (UUID fileId : fileIds) {
                filterBuilder.addShould(matchKeyword("fileId", fileId.toString()));
            }
            Filter filter = filterBuilder.build();

            // Build search request
            SearchPoints searchRequest = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(floatQuery)
                    .setFilter(filter)
                    .setLimit(topK)
                    .setWithPayload(
                            WithPayloadSelector.newBuilder()
                                    .setEnable(true)
                                    .build()
                    )
                    .build();

            List<ScoredPoint> results = qdrantClient.searchAsync(searchRequest).get();

            // Extract chunk text from payload
            List<String> relevantChunks = new ArrayList<>();
            for (ScoredPoint point : results) {
                if (point.getPayloadMap().containsKey("chunkText")) {
                    String chunkText = point.getPayloadMap().get("chunkText").getStringValue();
                    relevantChunks.add(chunkText);
                }
            }

            log.info("Qdrant search: {} results cho {} files (topK={})",
                    relevantChunks.size(), fileIds.size(), topK);

            return relevantChunks;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Qdrant search bị interrupted", e);
            return Collections.emptyList();
        } catch (ExecutionException e) {
            log.error("Lỗi search Qdrant: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * Xóa tất cả vector chunks của một file khỏi Qdrant.
     *
     * @param fileId UUID của file bị xóa
     */
    public void deleteByFileId(UUID fileId) {
        try {
            Filter filter = Filter.newBuilder()
                    .addMust(matchKeyword("fileId", fileId.toString()))
                    .build();

            qdrantClient.deleteAsync(collectionName, filter).get();

            log.info("Qdrant: đã xóa tất cả chunks cho file {}", fileId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Qdrant delete bị interrupted", e);
        } catch (ExecutionException e) {
            log.error("Lỗi xóa Qdrant vectors cho file {}: {}", fileId, e.getMessage(), e);
        }
    }

    // ── Check ────────────────────────────────────────────────────────────────

    /**
     * Kiểm tra xem file đã có chunks trong Qdrant chưa.
     *
     * @param fileId UUID của file
     * @return true nếu có ít nhất 1 chunk
     */
    public boolean hasChunks(UUID fileId) {
        try {
            Filter filter = Filter.newBuilder()
                    .addMust(matchKeyword("fileId", fileId.toString()))
                    .build();

            // Scroll with limit 1 to check existence
            List<ScoredPoint> results = qdrantClient.searchAsync(
                    SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(createZeroVector())
                            .setFilter(filter)
                            .setLimit(1)
                            .build()
            ).get();

            return !results.isEmpty();

        } catch (Exception e) {
            log.warn("Lỗi kiểm tra chunks Qdrant cho file {}: {}", fileId, e.getMessage());
            return false;
        }
    }

    private List<Float> createZeroVector() {
        List<Float> zeroVec = new ArrayList<>(VECTOR_DIMENSION);
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            zeroVec.add(0.0f);
        }
        return zeroVec;
    }
}
