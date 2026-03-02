package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {

    FileResponse uploadFile(MultipartFile file, Long userId);

    FileResponse getFileById(Long fileId);

    PageResponse<FileResponse> getMyFiles(Long userId, Pageable pageable);

    PageResponse<FileResponse> getAllFiles(Pageable pageable);

    void deleteFile(Long fileId, Long userId, boolean isAdmin);
}
