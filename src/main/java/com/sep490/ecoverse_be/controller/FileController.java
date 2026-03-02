package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.CloudinaryResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.ICloudinaryService;
import com.sep490.ecoverse_be.util.FileUpLoadUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File", description = "Upload file lên Cloudinary")
public class FileController {

    @Autowired
    private ICloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file ảnh, trả về URL")
    public ResponseDto<CloudinaryResponse> uploadFile(
            @RequestPart("file") MultipartFile file) {

        FileUpLoadUtil.assertAllowed(file, FileUpLoadUtil.IMAGE_PATTERN);
        String fileName = FileUpLoadUtil.getFileName(file.getOriginalFilename());
        CloudinaryResponse response = cloudinaryService.uploadFile(file, fileName);

        return ResponseDto.success(response, "Upload thành công");
    }
}
