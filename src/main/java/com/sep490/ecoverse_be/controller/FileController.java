package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.IFileService;
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

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<FileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FileResponse response = fileService.uploadFile(file, principal.getUser().getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseDto.created(response, "File uploaded successfully."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<FileResponse>> getFileById(@PathVariable Long id) {
        FileResponse response = fileService.getFileById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "File retrieved successfully."));
    }

    @GetMapping("/my")
    public ResponseEntity<ResponseDto<PageResponse<FileResponse>>> getMyFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        PageResponse<FileResponse> response = fileService.getMyFiles(
                principal.getUser().getId(), pageable
        );
        return ResponseEntity.ok(ResponseDto.success(response, "Files retrieved successfully."));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<PageResponse<FileResponse>>> getAllFiles(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        PageResponse<FileResponse> response = fileService.getAllFiles(pageable);
        return ResponseEntity.ok(ResponseDto.success(response, "All files retrieved successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        fileService.deleteFile(id, principal.getUser().getId(), isAdmin);
        return ResponseEntity.ok(ResponseDto.success(null, "File deleted successfully."));
    }
}
