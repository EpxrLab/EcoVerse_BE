package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.report.*;
import com.sep490.ecoverse_be.enums.ReportPeriod;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public interface IReportService {

    // ── Student ───────────────────────────────────────────────────────────────

    StudentReportSummaryResponse getStudentSummary(UUID userId, ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);

    StudentPerformanceResponse getStudentPerformance(UUID userId, ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);

    StudentCoinReportResponse getStudentCoinReport(UUID userId, ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);

    // ── School ────────────────────────────────────────────────────────────────

    SchoolReportSummaryResponse getSchoolSummary(UUID userId, ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);

    List<SchoolStudentRankResponse> getSchoolStudentRankings(UUID userId);

    List<SchoolCampaignReportResponse> getSchoolCampaigns(UUID userId);

    // ── Partnership ───────────────────────────────────────────────────────────

    PartnershipReportSummaryResponse getPartnershipSummary(UUID userId, ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);

    List<PartnershipCampaignReportResponse> getPartnershipCampaigns(UUID userId);

    // ── Admin ─────────────────────────────────────────────────────────────────

    AdminReportSummaryResponse getAdminSummary(ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);

    AdminRevenueReportResponse getAdminRevenue(ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate);
}
