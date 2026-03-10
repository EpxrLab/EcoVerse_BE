package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ICloudinaryService {
    // Upload ảnh thông thường (jpg, png, gif, bmp)
    CloudinaryResponse uploadFile(MultipartFile file, String fileName);

    // Upload 3D model (.glb, .gltf) dùng chunked upload + resource_type raw
    CloudinaryResponse uploadModelFile(MultipartFile file, String fileName);
}
