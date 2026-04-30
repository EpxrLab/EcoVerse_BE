package com.sep490.ecoverse_be.enums;

/**
 * Trạng thái embedding vector của file tài liệu trong Qdrant.
 */
public enum EmbeddingStatus {
    /** Chưa bắt đầu embed */
    PENDING,
    /** Đang trong quá trình embed */
    PROCESSING,
    /** Đã embed thành công, vector sẵn sàng trong Qdrant */
    COMPLETED,
    /** Embed thất bại */
    FAILED
}
