package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.EmbeddingStatus;
import com.sep490.ecoverse_be.enums.FileCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "files", indexes = {
        @Index(name = "idx_file_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_file_type", columnList = "file_type"),
        @Index(name = "idx_file_public_id", columnList = "public_id"),
        @Index(name = "idx_file_category", columnList = "file_category"),
        @Index(name = "idx_file_embedding_status", columnList = "embedding_status")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileEntity extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", length = 20)
    private FileCategory category;

    /**
     * Trạng thái embedding vector vào Qdrant.
     * Chỉ áp dụng cho DOCUMENT files. null = chưa áp dụng (file không phải document).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", length = 20)
    private EmbeddingStatus embeddingStatus;
}
