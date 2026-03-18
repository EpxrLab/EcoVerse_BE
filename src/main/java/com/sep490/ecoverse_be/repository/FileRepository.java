package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.FileEntity;
import com.sep490.ecoverse_be.enums.FileCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Page<FileEntity> findByUploadedById(UUID userId, Pageable pageable);

    Page<FileEntity> findByCategory(FileCategory category, Pageable pageable);

    Page<FileEntity> findByUploadedByIdAndCategory(UUID userId, FileCategory category, Pageable pageable);
}
