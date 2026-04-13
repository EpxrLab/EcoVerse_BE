package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.StorageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {

    StorageResponse uploadImageFile(MultipartFile file, String fileName);

    StorageResponse uploadModelFile(MultipartFile file, String fileName);

    StorageResponse uploadContractFile(MultipartFile file, String fileName);

    StorageResponse uploadDocumentFile(MultipartFile file, String fileName);

    void deleteFile(String s3Key);
}
