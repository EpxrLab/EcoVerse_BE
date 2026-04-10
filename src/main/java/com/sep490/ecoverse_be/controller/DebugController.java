package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionSummaryResponse;
import com.sep490.ecoverse_be.service.IStudentGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public debug controller - không yêu cầu xác thực.
 * Chỉ dùng cho mục đích debug trong quá trình phát triển.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final IStudentGameService studentGameService;

    /**
     * Lấy danh sách các game session đang mở (chưa submit) của một student.
     * GET /api/debug/students/{studentId}/open-sessions
     */
    @GetMapping("/students/{studentId}/open-sessions")
    public ResponseEntity<ResponseDto<List<StudentGameSessionSummaryResponse>>> getOpenSessions(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ResponseDto.success(
                studentGameService.getOpenSessionsByStudentId(studentId),
                "Lấy danh sách session đang mở thành công"
        ));
    }
}
