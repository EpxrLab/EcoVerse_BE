package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ICloudinaryService {
    CloudinaryResponse uploadFile(MultipartFile file, String fileName);
}
