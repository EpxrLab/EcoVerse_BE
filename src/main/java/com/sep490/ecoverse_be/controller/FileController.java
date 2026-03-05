package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.CloudinaryResponse;
import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ICloudinaryService;
import com.sep490.ecoverse_be.service.IFileService;
import com.sep490.ecoverse_be.util.FileUpLoadUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "Upload và quản lý file")
public class FileController {

    private final ICloudinaryService cloudinaryService;
    private final IFileService fileService;

    @PostMapping(value = "/upload/cloudinary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file ảnh lên Cloudinary, trả về URL")
    public ResponseDto<CloudinaryResponse> uploadToCloudinary(
            @RequestPart("file") MultipartFile file) {

        FileUpLoadUtil.assertAllowed(file, FileUpLoadUtil.IMAGE_PATTERN);
        String fileName = FileUpLoadUtil.getFileName(file.getOriginalFilename());
        CloudinaryResponse response = cloudinaryService.uploadFile(file, fileName);

        return ResponseDto.success(response, "Upload thành công");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file và lưu thông tin vào DB")
    public ResponseEntity<ResponseDto<FileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        FileResponse response = fileService.uploadFile(file, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseDto.created(response, "File uploaded successfully."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin file theo ID")
    public ResponseEntity<ResponseDto<FileResponse>> getFileById(@PathVariable UUID id) {
        FileResponse response = fileService.getFileById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "File retrieved successfully."));
    }

    @GetMapping("/my")
    @Operation(summary = "Lấy danh sách file của tôi")
    public ResponseEntity<ResponseDto<PageResponse<FileResponse>>> getMyFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        UUID userId = principal.getUser().getId();
        PageResponse<FileResponse> response = fileService.getMyFiles(userId, pageable);
        return ResponseEntity.ok(ResponseDto.success(response, "Files retrieved successfully."));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @Operation(summary = "Lấy tất cả file (Admin)")
    public ResponseEntity<ResponseDto<PageResponse<FileResponse>>> getAllFiles(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        PageResponse<FileResponse> response = fileService.getAllFiles(pageable);
        return ResponseEntity.ok(ResponseDto.success(response, "All files retrieved successfully."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa file theo ID")
    public ResponseEntity<ResponseDto<Void>> deleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal.getUser().getId();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        fileService.deleteFile(id, userId, isAdmin);
        return ResponseEntity.ok(ResponseDto.success(null, "File deleted successfully."));
    }
}
