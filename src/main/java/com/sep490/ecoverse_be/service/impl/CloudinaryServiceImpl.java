package com.sep490.ecoverse_be.service.impl;

import com.cloudinary.Cloudinary;
import com.sep490.ecoverse_be.dto.response.CloudinaryResponse;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.service.ICloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements ICloudinaryService {

    @Autowired
    Cloudinary cloudinary;

    // Kích thước mỗi chunk khi upload large file: 20MB
    private static final int CHUNK_SIZE = 20 * 1024 * 1024;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public CloudinaryResponse uploadFile(final MultipartFile file, final String fileName) {
        try {
            // Dùng InputStream thay getBytes() để tránh load toàn bộ file vào RAM
            final Map<String, Object> options = new HashMap<>();
            options.put("public_id", "ecoverse/user/" + fileName);

            final Map<?, ?> result = this.cloudinary.uploader()
                    .upload(file.getInputStream(), options);

            final String url = (String) result.get("secure_url");
            final String publicId = (String) result.get("public_id");
            return CloudinaryResponse.builder().publicId(publicId).url(url).build();
        } catch (Exception e) {
            throw new FuncErrorException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public CloudinaryResponse uploadModelFile(final MultipartFile file, final String fileName) {
        // Cloudinary Java SDK: uploadLarge() chỉ hỗ trợ chunked thật sự khi nhận java.io.File
        // Truyền InputStream sẽ vẫn upload 1 lần và bị giới hạn 10MB
        // Giải pháp: chuyển MultipartFile → temp File → uploadLarge → xóa temp File
        File tempFile = null;
        try {
            // Tạo temp file với đúng extension để Cloudinary nhận diện
            tempFile = File.createTempFile("ecoverse_model_", "_" + fileName);
            file.transferTo(tempFile);

            final Map<String, Object> options = new HashMap<>();
            options.put("public_id", "ecoverse/models/" + fileName);
            // resource_type "raw" bắt buộc — không set thì Cloudinary coi là ảnh, giới hạn 10MB
            options.put("resource_type", "raw");
            // chunk_size: chia nhỏ thành các phần 20MB, tránh timeout và vượt giới hạn single upload
            options.put("chunk_size", CHUNK_SIZE);

            final Map<?, ?> result = this.cloudinary.uploader()
                    .uploadLarge(tempFile, options);

            final String url = (String) result.get("secure_url");
            final String publicId = (String) result.get("public_id");
            return CloudinaryResponse.builder().publicId(publicId).url(url).build();
        } catch (IOException e) {
            throw new FuncErrorException("Không thể xử lý file 3D model: " + e.getMessage());
        } catch (Exception e) {
            throw new FuncErrorException("Failed to upload 3D model: " + e.getMessage());
        } finally {
            // Luôn xóa temp file dù thành công hay thất bại
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
