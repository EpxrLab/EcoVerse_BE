package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Page<FileEntity> findByUploadedById(Long userId, Pageable pageable);
}
