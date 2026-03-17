package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.StudentAccountInfo;
import com.sep490.ecoverse_be.service.ParentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parent")
@Tag(name = "Parent")
@PreAuthorize("hasAuthority('PARENT')")
public class ParentController {
    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping("/children")
    public ResponseEntity<ResponseDto<List<StudentAccountInfo>>> getChildren() {
        List<StudentAccountInfo> studentAccountInfoList =  parentService.getChildren();
        return ResponseEntity.ok(ResponseDto.success(studentAccountInfoList, "Danh sách các con của phụ huynh thành công"));
    }

}
