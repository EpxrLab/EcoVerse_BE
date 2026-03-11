package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.entity.FileEntity;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.FileMapper;
import com.sep490.ecoverse_be.repository.FileRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IFileService;
import com.sep490.ecoverse_be.util.FileUpLoadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3FileService s3FileService;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public FileResponse uploadFile(MultipartFile file, UUID userId) {
        if (file.isEmpty()) {
            throw new FuncErrorException("File must not be empty.");
        }

        if (file.getSize() > FileUpLoadUtil.MAX_FILE_SIZE) {
            throw new FuncErrorException("Max file size is 100MB.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Map<String, Object> uploadResult = s3FileService.upload(file);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFileUrl((String) uploadResult.get("secure_url"));
        fileEntity.setPublicId((String) uploadResult.get("public_id"));
        fileEntity.setFileType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setUploadedBy(user);

        FileEntity saved = fileRepository.save(fileEntity);
        return fileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(UUID fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        return fileMapper.toResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> getMyFiles(UUID userId, Pageable pageable) {
        Page<FileEntity> page = fileRepository.findByUploadedById(userId, pageable);
        return PageResponse.from(page, fileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> getAllFiles(Pageable pageable) {
        Page<FileEntity> page = fileRepository.findAll(pageable);
        return PageResponse.from(page, fileMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId, UUID userId, boolean isAdmin) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        if (!isAdmin && !file.getUploadedBy().getId().equals(userId)) {
            throw new FuncErrorException("You do not have permission to delete this file.");
        }

        s3FileService.delete(file.getPublicId());
        fileRepository.delete(file);
    }
}
