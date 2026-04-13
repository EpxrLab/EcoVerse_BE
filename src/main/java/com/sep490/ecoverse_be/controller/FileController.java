package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.StorageResponse;
import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    private final IFileService fileService;

    // ===================== UPLOAD ENDPOINTS =====================

    @PostMapping(value = "/upload/contract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload hợp đồng (public, không cần đăng nhập)",
            description = "Upload file hợp đồng (.pdf, .docx, .doc) khi đăng ký. "
                    + "Không cần đăng nhập. Tối đa 50MB.")
    public ResponseEntity<ResponseDto<StorageResponse>> uploadContract(
            @RequestPart("file") MultipartFile file) {
        StorageResponse response = fileService.uploadContract(file);
        return ResponseEntity.ok(ResponseDto.success(response, "Upload hợp đồng thành công"));
    }

    @PostMapping(value = "/upload/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @Operation(summary = "Upload 3D model (chỉ Admin)",
            description = "Upload file 3D model (.glb, .gltf). Chỉ Admin. Tối đa 250MB. "
                    + "File lưu tại `ecoverse/models/` trên S3.")
    public ResponseEntity<ResponseDto<StorageResponse>> uploadModel(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        StorageResponse response = fileService.uploadModel(file, userId);
        return ResponseEntity.ok(ResponseDto.success(response, "Upload 3D model thành công"));
    }

    @PostMapping(value = "/upload/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload tài liệu (cần đăng nhập)",
            description = "Upload file tài liệu (.pdf, .docx, .doc, .xls, .xlsx, .ppt, .pptx). "
                    + "Cần đăng nhập. Tối đa 100MB. Chỉ người upload mới xem được.")
    public ResponseEntity<ResponseDto<StorageResponse>> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        StorageResponse response = fileService.uploadDocument(file, userId);
        return ResponseEntity.ok(ResponseDto.success(response, "Upload tài liệu thành công"));
    }

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh (cần đăng nhập)",
            description = "Upload file ảnh (.jpg, .png, .gif, .bmp). "
                    + "Cần đăng nhập. Tối đa 100MB. Ai cũng xem được.")
    public ResponseEntity<ResponseDto<StorageResponse>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        StorageResponse response = fileService.uploadImage(file, userId);
        return ResponseEntity.ok(ResponseDto.success(response, "Upload ảnh thành công"));
    }

    // ===================== VIEW ENDPOINT (phân quyền theo category) =====================

    @GetMapping("/view/{id}")
    @Operation(summary = "Xem/tải file theo ID (phân quyền theo loại file)",
            description = "IMAGE/MODEL: ai cũng xem được. "
                    + "CONTRACT: chỉ Admin. "
                    + "DOCUMENT: chỉ người upload hoặc Admin.")
    public ResponseEntity<ResponseDto<FileResponse>> viewFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = (principal != null) ? principal.getUser().getId() : null;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRATOR"));
        FileResponse response = fileService.viewFile(id, userId, isAdmin);
        return ResponseEntity.ok(ResponseDto.success(response, "File retrieved successfully."));
    }

    // ===================== QUẢN LÝ FILE =====================

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin file theo ID (cần đăng nhập)")
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
    @Operation(summary = "Lấy tất cả file (chỉ Admin)")
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
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRATOR"));
        fileService.deleteFile(id, userId, isAdmin);
        return ResponseEntity.ok(ResponseDto.success(null, "File deleted successfully."));
    }
}
