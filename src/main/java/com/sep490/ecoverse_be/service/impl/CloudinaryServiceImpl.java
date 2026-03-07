package com.sep490.ecoverse_be.service.impl;

import com.cloudinary.Cloudinary;
import com.sep490.ecoverse_be.dto.response.CloudinaryResponse;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.service.ICloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryServiceImpl implements ICloudinaryService {

    @Autowired
    Cloudinary cloudinary;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public CloudinaryResponse uploadFile(final MultipartFile file, final String fileName) {
        try {
            final Map<String, Object> result = this.cloudinary.uploader()
                    .upload(file.getBytes(),
                            Map.of("public_id",
                                    "ecoverse/user/"
                                            + fileName));
            final String url = (String) result.get("secure_url");
            final String publicId = (String) result.get("public_id");
            return CloudinaryResponse.builder().publicId(publicId).url(url).build();
        } catch (Exception e) {
            throw new FuncErrorException("Failed to upload file");
        }
    }
}
