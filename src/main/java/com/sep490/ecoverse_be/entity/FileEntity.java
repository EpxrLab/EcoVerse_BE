package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "files", indexes = {
        @Index(name = "idx_file_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_file_type", columnList = "file_type"),
        @Index(name = "idx_file_public_id", columnList = "public_id")
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
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
}
