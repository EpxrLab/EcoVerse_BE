package com.sep490.ecoverse_be.mapper;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.entity.FileEntity;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    public FileResponse toResponse(FileEntity file) {
        return new FileResponse(
                file.getId(),
                file.getFileName(),
                file.getFileUrl(),
                file.getFileType(),
                file.getFileSize(),
                file.getUploadedBy() != null ? file.getUploadedBy().getEmail() : null,
                file.getCreatedAt()
        );
    }
}
