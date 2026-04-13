package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.report.*;
import com.sep490.ecoverse_be.enums.ReportPeriod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IReportService {

    // ── Student ───────────────────────────────────────────────────────────────

    StudentReportSummaryResponse getStudentSummary(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);

    StudentPerformanceResponse getStudentPerformance(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);

    StudentCoinReportResponse getStudentCoinReport(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);

    // ── School ────────────────────────────────────────────────────────────────

    SchoolReportSummaryResponse getSchoolSummary(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);

    List<SchoolStudentRankResponse> getSchoolStudentRankings(UUID userId);

    List<SchoolCampaignReportResponse> getSchoolCampaigns(UUID userId);

    // ── Partnership ───────────────────────────────────────────────────────────

    PartnershipReportSummaryResponse getPartnershipSummary(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);

    List<PartnershipCampaignReportResponse> getPartnershipCampaigns(UUID userId);

    // ── Admin ─────────────────────────────────────────────────────────────────

    AdminReportSummaryResponse getAdminSummary(ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);

    AdminRevenueReportResponse getAdminRevenue(ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate);
}
