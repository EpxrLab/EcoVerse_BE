package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.StorageResponse;
import com.sep490.ecoverse_be.entity.FileEntity;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.EmbeddingStatus;
import com.sep490.ecoverse_be.enums.FileCategory;
import com.sep490.ecoverse_be.event.DocumentUploadedEvent;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.FileMapper;
import com.sep490.ecoverse_be.repository.FileRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IFileService;
import com.sep490.ecoverse_be.service.IStorageService;
import com.sep490.ecoverse_be.util.FileUpLoadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final IStorageService storageService;
    private final FileMapper fileMapper;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final ApplicationEventPublisher eventPublisher;
    private final QdrantVectorStoreService qdrantVectorStoreService;

    @Override
    @Transactional
    public StorageResponse uploadImage(MultipartFile file, UUID userId) {
        FileUpLoadUtil.assertAllowed(file, FileUpLoadUtil.IMAGE_PATTERN);
        String fileName = FileUpLoadUtil.getFileName(file.getOriginalFilename());
        StorageResponse response = storageService.uploadImageFile(file, fileName);
        enrichPresignedUrl(response);
        saveFileEntity(file, response, userId, FileCategory.IMAGE);
        return response;
    }

    @Override
    @Transactional
    public StorageResponse uploadModel(MultipartFile file, UUID userId) {
        FileUpLoadUtil.assertModelAllowed(file);
        String fileName = FileUpLoadUtil.getFileName(file.getOriginalFilename());
        StorageResponse response = storageService.uploadModelFile(file, fileName);
        enrichPresignedUrl(response);
        saveFileEntity(file, response, userId, FileCategory.MODEL);
        return response;
    }

    @Override
    @Transactional
    public StorageResponse uploadContract(MultipartFile file) {
        FileUpLoadUtil.assertContractAllowed(file);
        String fileName = FileUpLoadUtil.getFileName(file.getOriginalFilename());
        StorageResponse response = storageService.uploadContractFile(file, fileName);
        enrichPresignedUrl(response);
        saveFileEntity(file, response, null, FileCategory.CONTRACT);
        return response;
    }

    @Override
    @Transactional
    public StorageResponse uploadDocument(MultipartFile file, UUID userId) {
        FileUpLoadUtil.assertDocumentAllowed(file);
        String fileName = FileUpLoadUtil.getFileName(file.getOriginalFilename());
        StorageResponse response = storageService.uploadDocumentFile(file, fileName);
        enrichPresignedUrl(response);
        FileEntity savedFile = saveFileEntity(file, response, userId, FileCategory.DOCUMENT);

        // Publish event để auto-embed document vào Qdrant (async)
        eventPublisher.publishEvent(new DocumentUploadedEvent(this, savedFile.getId()));

        return response;
    }

    private void enrichPresignedUrl(StorageResponse response) {
        String presignedUrl = s3PresignedUrlService.generatePresignedUrl(response.getPublicId());
        response.setPresignedUrl(presignedUrl);
    }

    private FileEntity saveFileEntity(MultipartFile file, StorageResponse response,
                                UUID userId, FileCategory category) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFileUrl(response.getPublicId());
        fileEntity.setPublicId(response.getPublicId());
        fileEntity.setFileType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setUploadedBy(user);
        fileEntity.setCategory(category);

        // Đánh dấu PENDING embedding cho document files
        if (category == FileCategory.DOCUMENT) {
            fileEntity.setEmbeddingStatus(EmbeddingStatus.PENDING);
        }

        return fileRepository.save(fileEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse viewFile(UUID fileId, UUID userId, boolean isAdmin) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        FileCategory category = file.getCategory();

        // IMAGE, MODEL: public — ai cũng xem được
        if (category == FileCategory.IMAGE || category == FileCategory.MODEL) {
            return fileMapper.toResponse(file);
        }

        // CONTRACT: chỉ Admin
        if (category == FileCategory.CONTRACT) {
            if (!isAdmin) {
                throw new FuncErrorException("You do not have permission to view this file.");
            }
            return fileMapper.toResponse(file);
        }

        // DOCUMENT: owner hoặc Admin
        if (category == FileCategory.DOCUMENT) {
            boolean isOwner = file.getUploadedBy() != null
                    && userId != null
                    && file.getUploadedBy().getId().equals(userId);
            if (!isOwner && !isAdmin) {
                throw new FuncErrorException("You do not have permission to view this file.");
            }
            return fileMapper.toResponse(file);
        }

        // Fallback cho file cũ chưa có category
        return fileMapper.toResponse(file);
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

        if (!isAdmin) {
            if (file.getUploadedBy() == null) {
                throw new FuncErrorException("You do not have permission to delete this file.");
            }
            if (!file.getUploadedBy().getId().equals(userId)) {
                throw new FuncErrorException("You do not have permission to delete this file.");
            }
        }

        // Xóa vectors trong Qdrant nếu file là DOCUMENT đã được embed
        if (file.getCategory() == FileCategory.DOCUMENT
                && file.getEmbeddingStatus() == EmbeddingStatus.COMPLETED) {
            qdrantVectorStoreService.deleteByFileId(fileId);
        }

        storageService.deleteFile(file.getPublicId());
        fileRepository.delete(file);
    }
}

