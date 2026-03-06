package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.request.ApprovalRequest;
import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.service.IAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
@Tag(name = "Admin", description = "Quản lý phê duyệt tài khoản trường học và đối tác")
public class AdminController {

    @Autowired
    private IAdminService adminService;

    @GetMapping("/schools/pending")
    @Operation(summary = "Lấy danh sách trường học đang chờ duyệt")
    public ResponseDto<List<SchoolDetailResponse>> getPendingSchools() {
        return ResponseDto.success(adminService.getPendingSchools(), "Danh sách trường học chờ duyệt");
    }

    @GetMapping("/partnerships/pending")
    @Operation(summary = "Lấy danh sách đối tác đang chờ duyệt")
    public ResponseDto<List<PartnershipDetailResponse>> getPendingPartnerships() {
        return ResponseDto.success(adminService.getPendingPartnerships(), "Danh sách đối tác chờ duyệt");
    }

    @PutMapping("/schools/{id}/approve")
    @Operation(summary = "Duyệt tài khoản trường học")
    public ResponseDto<SchoolDetailResponse> approveSchool(@PathVariable UUID id, @RequestBody UpdateApprovalRequest request) {
        try {
            return ResponseDto.success(adminService.updateSchoolApproval(id,request), "Duyệt trường học thành công");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseDto.badRequest(null, e.getMessage());
        }
    }

    @PutMapping("/partnerships/{id}/approve")
    @Operation(summary = "Duyệt tài khoản đối tác")
    public ResponseDto<PartnershipDetailResponse> approvePartnership(@PathVariable UUID id, @RequestBody UpdateApprovalRequest request) {
        try {
            return ResponseDto.success(adminService.updatePartnershipApproval(id, request), "Duyệt đối tác thành công");
        } catch (NotFoundException e) {
            return ResponseDto.notFound(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseDto.badRequest(null, e.getMessage());
        }
    }
}
