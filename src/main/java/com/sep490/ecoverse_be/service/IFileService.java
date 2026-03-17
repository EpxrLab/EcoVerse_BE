package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.StorageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IFileService {

    StorageResponse uploadImage(MultipartFile file, UUID userId);

    StorageResponse uploadModel(MultipartFile file, UUID userId);

    FileResponse getFileById(UUID fileId);

    PageResponse<FileResponse> getMyFiles(UUID userId, Pageable pageable);

    PageResponse<FileResponse> getAllFiles(Pageable pageable);

    void deleteFile(UUID fileId, UUID userId, boolean isAdmin);
}
