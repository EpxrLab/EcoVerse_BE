package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.dto.response.report.*;
import com.sep490.ecoverse_be.enums.ReportPeriod;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.IReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/report")
@Tag(name = "Report", description = "Thống kê báo cáo theo role (Student, School, Partnership)")
public class ReportController {

    @Autowired
    private IReportService reportService;

    // ── Student endpoints ────────────────────────────────────────────────────

    @GetMapping("/student/summary")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Tổng quan báo cáo học sinh", description = """
            Trả về tổng quan thống kê cho học sinh đang đăng nhập.

            **Bao gồm:** Coin (số dư, đã kiếm, đã tiêu), Chiến dịch (tổng, đang diễn ra),
            Game (số session, accuracy trung bình, tốt nhất), Quiz (số lượt, điểm TB, tỉ lệ pass),
            Xếp hạng tốt nhất, Danh hiệu, Đổi thưởng.

            **period:** `THIS_WEEK | THIS_MONTH | LAST_3_MONTHS | THIS_YEAR | CUSTOM`
            Khi `CUSTOM`, cần truyền thêm `fromDate` và `toDate`.
            """)
    public ResponseEntity<ResponseDto<StudentReportSummaryResponse>> getStudentSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "THIS_MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getStudentSummary(userId, period, fromDate, toDate),
                "Lấy báo cáo tổng quan học sinh thành công"));
    }

    @GetMapping("/student/performance")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Hiệu suất học tập chi tiết của học sinh", description = """
            Trả về chi tiết hiệu suất Game và Quiz theo kỳ, kèm breakdown theo từng chiến dịch đã tham gia.
            """)
    public ResponseEntity<ResponseDto<StudentPerformanceResponse>> getStudentPerformance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "THIS_MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getStudentPerformance(userId, period, fromDate, toDate),
                "Lấy báo cáo hiệu suất học sinh thành công"));
    }

    @GetMapping("/student/coins")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Lịch sử Coin của học sinh", description = """
            Trả về thống kê Coin (kiếm, tiêu) và danh sách giao dịch trong kỳ được chọn.
            """)
    public ResponseEntity<ResponseDto<StudentCoinReportResponse>> getStudentCoinReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "THIS_MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getStudentCoinReport(userId, period, fromDate, toDate),
                "Lấy báo cáo Coin học sinh thành công"));
    }

    // ── School endpoints ─────────────────────────────────────────────────────

    @GetMapping("/school/summary")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(summary = "Tổng quan báo cáo trường học", description = """
            Trả về tổng quan quản lý trường:
            tổng học sinh, chiến dịch đã tạo/tham gia, yêu cầu đổi thưởng,
            trạng thái subscription, top 5 học sinh theo Coin và theo accuracy.
            """)
    public ResponseEntity<ResponseDto<SchoolReportSummaryResponse>> getSchoolSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "THIS_MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getSchoolSummary(userId, period, fromDate, toDate),
                "Lấy báo cáo trường học thành công"));
    }

    @GetMapping("/school/students")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(summary = "Bảng xếp hạng học sinh toàn trường", description = """
            Trả về danh sách tất cả học sinh của trường kèm chỉ số hiệu suất,
            sắp xếp theo tổng Coin giảm dần.
            """)
    public ResponseEntity<ResponseDto<List<SchoolStudentRankResponse>>> getSchoolStudentRankings(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getSchoolStudentRankings(userId),
                "Lấy bảng xếp hạng học sinh thành công"));
    }

    @GetMapping("/school/campaigns")
    @PreAuthorize("hasAuthority('PARTNERSHIP_SCHOOL')")
    @Operation(summary = "Danh sách chiến dịch của trường", description = """
            Trả về tất cả chiến dịch liên quan đến trường (tự tạo + tham gia từ đối tác),
            kèm thống kê số học sinh và accuracy trung bình.
            """)
    public ResponseEntity<ResponseDto<List<SchoolCampaignReportResponse>>> getSchoolCampaigns(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getSchoolCampaigns(userId),
                "Lấy danh sách chiến dịch trường thành công"));
    }

    // ── Partnership endpoints ────────────────────────────────────────────────

    @GetMapping("/partnership/summary")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(summary = "Tổng quan báo cáo đối tác", description = """
            Trả về tổng quan tầm phủ chiến dịch của đối tác:
            tổng chiến dịch, số trường tham gia, tổng học sinh tiếp cận,
            accuracy trung bình, trạng thái subscription, top 5 trường tích cực nhất.
            """)
    public ResponseEntity<ResponseDto<PartnershipReportSummaryResponse>> getPartnershipSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "THIS_MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getPartnershipSummary(userId, period, fromDate, toDate),
                "Lấy báo cáo đối tác thành công"));
    }

    @GetMapping("/partnership/campaigns")
    @PreAuthorize("hasAuthority('THIRD_PARTY_PARTNERSHIP')")
    @Operation(summary = "Danh sách chiến dịch của đối tác", description = """
            Trả về tất cả chiến dịch đã tạo bởi đối tác,
            kèm số trường tham gia, tổng học sinh, accuracy trung bình.
            """)
    public ResponseEntity<ResponseDto<List<PartnershipCampaignReportResponse>>> getPartnershipCampaigns(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(ResponseDto.success(
                reportService.getPartnershipCampaigns(userId),
                "Lấy danh sách chiến dịch đối tác thành công"));
    }
}
