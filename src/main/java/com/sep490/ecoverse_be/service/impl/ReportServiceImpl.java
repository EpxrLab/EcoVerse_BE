package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.report.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements IReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int TOP_STUDENTS_LIMIT = 5;
    private static final int TOP_SCHOOLS_LIMIT = 5;

    @Autowired private StudentRepository studentRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private PartnershipRepository partnershipRepository;
    @Autowired private UserRepository userRepository;

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignParticipantRepository campaignParticipantRepository;
    @Autowired private CampaignSchoolParticipateRepository campaignSchoolParticipateRepository;

    @Autowired private GameSessionRepository gameSessionRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private CoinTransactionRepository coinTransactionRepository;
    @Autowired private RoundLeaderboardRepository roundLeaderboardRepository;

    @Autowired private RewardRequestRepository rewardRequestRepository;
    @Autowired private StudentTitleRepository studentTitleRepository;

    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PaymentRepository paymentRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Student requireStudent(UUID userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));
    }

    private School requireSchool(UUID userId) {
        return schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
    }

    private Partnership requirePartnership(UUID userId) {
        return partnershipRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác"));
    }

    private LocalDateTime[] resolveRange(ReportPeriod period, LocalDateTime from, LocalDateTime to) {
        return ReportPeriod.resolveDateRange(period, from, to);
    }

    private String formatPeriodLabel(ReportPeriod period) {
        return period != null ? period.name() : "CUSTOM";
    }

    private double safeDouble(Double value) {
        return value != null ? Math.round(value * 100.0) / 100.0 : 0.0;
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    // ── Student ───────────────────────────────────────────────────────────────

    @Override
    public StudentReportSummaryResponse getStudentSummary(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        Student student = requireStudent(userId);
        UUID studentId = student.getId();
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        List<TransactionType> earnTypes = List.of(TransactionType.EARN_GAME, TransactionType.EARN_QUIZ, TransactionType.EARN_TITLE);
        List<TransactionType> spendTypes = List.of(TransactionType.SPEND_REWARD);

        BigDecimal totalEarned = safeBigDecimal(coinTransactionRepository.sumByStudentIdAndTypes(studentId, earnTypes));
        BigDecimal totalSpent = safeBigDecimal(coinTransactionRepository.sumByStudentIdAndTypes(studentId, spendTypes));

        long totalCampaigns = campaignParticipantRepository.findByStudentIdAndIsActiveTrue(studentId).size();
        long activeCampaigns = campaignParticipantRepository.findByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(studentId)
                .stream().filter(CampaignParticipant::isActive).count();

        long totalGames = gameSessionRepository.countCompletedByStudentId(studentId);
        Double avgGameAcc = gameSessionRepository.avgAccuracyByStudentId(studentId);
        BigDecimal bestGameAcc = gameSessionRepository.maxAccuracyByStudentId(studentId);

        long totalQuizzes = quizAttemptRepository.countCompletedByStudentId(studentId);
        long passedQuizzes = quizAttemptRepository.countPassedByStudentId(studentId);
        Double avgQuizScore = quizAttemptRepository.avgScoreByStudentId(studentId);
        double passRate = totalQuizzes > 0 ? ((double) passedQuizzes / totalQuizzes) * 100.0 : 0.0;

        Integer bestRank = roundLeaderboardRepository.findBestRankByStudentId(studentId);
        long totalTitles = studentTitleRepository.countByStudentId(studentId);
        long totalRewards = rewardRequestRepository.countByStudentId(studentId);
        long pendingRewards = rewardRequestRepository.countByStudentIdAndStatus(studentId, RewardRequestStatus.PENDING);

        return StudentReportSummaryResponse.builder()
                .fullName(student.getFullName())
                .className(student.getClassName())
                .gradeLevel(student.getGradeLevel())
                .currentCoinBalance(safeBigDecimal(student.getTotalCoins()))
                .totalCoinsEarned(totalEarned)
                .totalCoinsSpent(totalSpent)
                .totalCampaignsJoined(totalCampaigns)
                .activeCampaigns(activeCampaigns)
                .totalGameSessionsCompleted(totalGames)
                .avgGameAccuracy(safeDouble(avgGameAcc))
                .bestGameAccuracy(safeBigDecimal(bestGameAcc))
                .totalQuizAttemptsCompleted(totalQuizzes)
                .avgQuizScore(safeDouble(avgQuizScore))
                .quizPassRate(Math.round(passRate * 100.0) / 100.0)
                .bestOverallRank(bestRank)
                .totalTitlesEarned(totalTitles)
                .totalRewardRequestsMade(totalRewards)
                .pendingRewardRequests(pendingRewards)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    @Override
    public StudentPerformanceResponse getStudentPerformance(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        Student student = requireStudent(userId);
        UUID studentId = student.getId();
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        long totalGames = gameSessionRepository.countCompletedByStudentId(studentId);
        Double avgGameAcc = gameSessionRepository.avgAccuracyByStudentId(studentId);
        BigDecimal bestGameAcc = gameSessionRepository.maxAccuracyByStudentId(studentId);
        long gamesInPeriod = gameSessionRepository.countCompletedByStudentIdAndDateRange(studentId, from, to);
        Double avgGameAccInPeriod = gameSessionRepository.avgAccuracyByStudentIdAndDateRange(studentId, from, to);

        long totalQuizzes = quizAttemptRepository.countCompletedByStudentId(studentId);
        long passedQuizzes = quizAttemptRepository.countPassedByStudentId(studentId);
        Double avgQuizScore = quizAttemptRepository.avgScoreByStudentId(studentId);
        double passRate = totalQuizzes > 0 ? ((double) passedQuizzes / totalQuizzes) * 100.0 : 0.0;
        long quizzesInPeriod = quizAttemptRepository.countCompletedByStudentIdAndDateRange(studentId, from, to);
        Double avgQuizInPeriod = quizAttemptRepository.avgScoreByStudentIdAndDateRange(studentId, from, to);

        List<StudentPerformanceResponse.CampaignPerformanceDto> breakdown = buildStudentCampaignBreakdown(student);

        return StudentPerformanceResponse.builder()
                .totalGameSessionsCompleted(totalGames)
                .avgGameAccuracy(safeDouble(avgGameAcc))
                .bestGameAccuracy(safeBigDecimal(bestGameAcc))
                .totalGameSessionsInPeriod(gamesInPeriod)
                .avgGameAccuracyInPeriod(safeDouble(avgGameAccInPeriod))
                .totalQuizAttemptsCompleted(totalQuizzes)
                .avgQuizScore(safeDouble(avgQuizScore))
                .quizPassRate(Math.round(passRate * 100.0) / 100.0)
                .totalQuizAttemptsInPeriod(quizzesInPeriod)
                .avgQuizScoreInPeriod(safeDouble(avgQuizInPeriod))
                .campaignBreakdown(breakdown)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    private List<StudentPerformanceResponse.CampaignPerformanceDto> buildStudentCampaignBreakdown(Student student) {
        List<CampaignParticipant> participants = campaignParticipantRepository.findByStudentIdAndIsActiveTrue(student.getId());
        List<StudentPerformanceResponse.CampaignPerformanceDto> result = new ArrayList<>();

        for (CampaignParticipant cp : participants) {
            Campaign campaign = cp.getCampaign();
            String campaignStatus = campaign.getCampaignType() == CampaignType.SCHOOL_INTERNAL
                    ? (campaign.getSchoolStatus() != null ? campaign.getSchoolStatus().name() : "")
                    : (campaign.getPartnershipStatus() != null ? campaign.getPartnershipStatus().name() : "");

            List<RoundLeaderboard> lbEntries = roundLeaderboardRepository.findByCampaignId(campaign.getId())
                    .stream().filter(rl -> rl.getStudent().getId().equals(student.getId())).toList();

            Integer overallRank = lbEntries.stream().map(RoundLeaderboard::getOverallRankInRound).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            Integer schoolRank = lbEntries.stream().map(RoundLeaderboard::getSchoolRankInRound).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            BigDecimal combinedAcc = lbEntries.stream().map(RoundLeaderboard::getCombinedAccuracyPercentage).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!lbEntries.isEmpty() && combinedAcc.compareTo(BigDecimal.ZERO) > 0) {
                combinedAcc = combinedAcc.divide(BigDecimal.valueOf(lbEntries.size()), 2, RoundingMode.HALF_UP);
            }
            long gamesCompleted = lbEntries.stream().mapToLong(RoundLeaderboard::getGamesCompleted).sum();
            long quizzesCompleted = lbEntries.stream().mapToLong(RoundLeaderboard::getQuizzesCompleted).sum();
            Integer coinsEarned = lbEntries.stream().map(RoundLeaderboard::getTotalCoinsEarned).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();

            result.add(StudentPerformanceResponse.CampaignPerformanceDto.builder()
                    .campaignCode(campaign.getCampaignCode())
                    .campaignName(campaign.getCampaignName())
                    .campaignType(campaign.getCampaignType().name())
                    .campaignStatus(campaignStatus)
                    .overallRank(overallRank)
                    .schoolRank(schoolRank)
                    .combinedAccuracy(combinedAcc)
                    .gamesCompleted(gamesCompleted)
                    .quizzesCompleted(quizzesCompleted)
                    .totalCoinsEarned(coinsEarned > 0 ? coinsEarned : null)
                    .build());
        }
        return result;
    }

    @Override
    public StudentCoinReportResponse getStudentCoinReport(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        Student student = requireStudent(userId);
        UUID studentId = student.getId();
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        List<TransactionType> earnTypes = List.of(TransactionType.EARN_GAME, TransactionType.EARN_QUIZ, TransactionType.EARN_TITLE);
        List<TransactionType> spendTypes = List.of(TransactionType.SPEND_REWARD);

        BigDecimal earnedInPeriod = safeBigDecimal(coinTransactionRepository.sumByStudentIdAndTypesAndDateRange(studentId, earnTypes, from, to));
        BigDecimal spentInPeriod = safeBigDecimal(coinTransactionRepository.sumByStudentIdAndTypesAndDateRange(studentId, spendTypes, from, to));
        BigDecimal totalEarned = safeBigDecimal(coinTransactionRepository.sumByStudentIdAndTypes(studentId, earnTypes));
        BigDecimal totalSpent = safeBigDecimal(coinTransactionRepository.sumByStudentIdAndTypes(studentId, spendTypes));

        List<CoinTransaction> txns = coinTransactionRepository.findByStudentIdAndDateRange(studentId, from, to);
        List<StudentCoinReportResponse.CoinTransactionDto> txnDtos = txns.stream().map(t ->
                StudentCoinReportResponse.CoinTransactionDto.builder()
                        .id(t.getId())
                        .transactionType(t.getTransactionType())
                        .amount(safeBigDecimal(t.getAmount()))
                        .balanceBefore(safeBigDecimal(t.getBalanceBefore()))
                        .balanceAfter(safeBigDecimal(t.getBalanceAfter()))
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .build()
        ).collect(Collectors.toList());

        return StudentCoinReportResponse.builder()
                .currentBalance(safeBigDecimal(student.getTotalCoins()))
                .totalEarnedInPeriod(earnedInPeriod)
                .totalSpentInPeriod(spentInPeriod)
                .totalEarnedAllTime(totalEarned)
                .totalSpentAllTime(totalSpent)
                .transactions(txnDtos)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    // ── School ────────────────────────────────────────────────────────────────

    @Override
    public SchoolReportSummaryResponse getSchoolSummary(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        School school = requireSchool(userId);
        UUID schoolId = school.getId();
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        long totalStudents = studentRepository.countBySchoolId(schoolId);

        long totalCampaignsCreated = campaignRepository.countByCreatorSchoolId(schoolId);
        long activeCampaigns = campaignRepository.countByCreatorSchoolIdAndSchoolStatusIn(schoolId,
                List.of(SchoolCampaignStatus.ON_GOING, SchoolCampaignStatus.INVITING));
        long completedCampaigns = campaignRepository.countByCreatorSchoolIdAndSchoolStatusIn(schoolId,
                List.of(SchoolCampaignStatus.COMPLETED));
        long participated = campaignSchoolParticipateRepository.countBySchoolIdAndStatus(schoolId, ParticipationStatus.APPROVED);

        long pendingRewards = rewardRequestRepository.countBySchoolIdAndStatus(schoolId, RewardRequestStatus.PENDING);
        long processedRewards = rewardRequestRepository.countBySchoolIdAndStatus(schoolId, RewardRequestStatus.DELIVERED)
                + rewardRequestRepository.countBySchoolIdAndStatus(schoolId, RewardRequestStatus.CONFIRMED);

        Subscription activeSub = subscriptionRepository.findBySchoolIdAndStatus(schoolId, SubscriptionStatus.ACTIVE).orElse(null);

        List<SchoolStudentRankResponse> topByCoins = buildTopStudentsByCoins(schoolId);
        List<SchoolStudentRankResponse> topByAccuracy = buildTopStudentsByAccuracy(schoolId);

        return SchoolReportSummaryResponse.builder()
                .totalStudents(totalStudents)
                .totalCampaignsCreated(totalCampaignsCreated)
                .activeCampaignsCreated(activeCampaigns)
                .completedCampaignsCreated(completedCampaigns)
                .totalCampaignsParticipated(participated)
                .pendingRewardRequests(pendingRewards)
                .totalRewardRequestsProcessed(processedRewards)
                .subscriptionStatus(activeSub != null ? activeSub.getStatus() : null)
                .subscriptionEndDate(activeSub != null ? activeSub.getEndDate() : null)
                .subscriptionPlanName(activeSub != null && activeSub.getPlan() != null ? activeSub.getPlan().getPlanName() : null)
                .topStudentsByCoins(topByCoins)
                .topStudentsByAccuracy(topByAccuracy)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    private List<SchoolStudentRankResponse> buildTopStudentsByCoins(UUID schoolId) {
        List<Student> top = studentRepository.findTopBySchoolIdOrderByTotalCoinsDesc(schoolId, PageRequest.of(0, TOP_STUDENTS_LIMIT));
        return top.stream().map(s -> SchoolStudentRankResponse.builder()
                .studentId(s.getId())
                .fullName(s.getFullName())
                .className(s.getClassName())
                .gradeLevel(s.getGradeLevel())
                .totalCoins(safeBigDecimal(s.getTotalCoins()))
                .avgGameAccuracy(safeDouble(gameSessionRepository.avgAccuracyByStudentId(s.getId())))
                .avgQuizScore(safeDouble(quizAttemptRepository.avgScoreByStudentId(s.getId())))
                .totalCampaignsJoined(campaignParticipantRepository.findByStudentIdAndIsActiveTrue(s.getId()).size())
                .totalTitlesEarned(studentTitleRepository.countByStudentId(s.getId()))
                .build()).collect(Collectors.toList());
    }

    private List<SchoolStudentRankResponse> buildTopStudentsByAccuracy(UUID schoolId) {
        List<Object[]> rows = gameSessionRepository.findTopStudentsByGameAccuracyInSchool(schoolId, PageRequest.of(0, TOP_STUDENTS_LIMIT));
        List<SchoolStudentRankResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID sid = (UUID) row[0];
            Double avgAcc = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            studentRepository.findById(sid).ifPresent(s ->
                    result.add(SchoolStudentRankResponse.builder()
                            .studentId(sid)
                            .fullName(s.getFullName())
                            .className(s.getClassName())
                            .gradeLevel(s.getGradeLevel())
                            .totalCoins(safeBigDecimal(s.getTotalCoins()))
                            .avgGameAccuracy(Math.round(avgAcc * 100.0) / 100.0)
                            .avgQuizScore(safeDouble(quizAttemptRepository.avgScoreByStudentId(sid)))
                            .totalCampaignsJoined(campaignParticipantRepository.findByStudentIdAndIsActiveTrue(sid).size())
                            .totalTitlesEarned(studentTitleRepository.countByStudentId(sid))
                            .build()));
        }
        return result;
    }

    @Override
    public List<SchoolStudentRankResponse> getSchoolStudentRankings(UUID userId) {
        School school = requireSchool(userId);
        List<Student> students = studentRepository.findBySchoolId(school.getId());
        return students.stream().map(s -> SchoolStudentRankResponse.builder()
                .studentId(s.getId())
                .fullName(s.getFullName())
                .className(s.getClassName())
                .gradeLevel(s.getGradeLevel())
                .totalCoins(safeBigDecimal(s.getTotalCoins()))
                .avgGameAccuracy(safeDouble(gameSessionRepository.avgAccuracyByStudentId(s.getId())))
                .avgQuizScore(safeDouble(quizAttemptRepository.avgScoreByStudentId(s.getId())))
                .totalCampaignsJoined(campaignParticipantRepository.findByStudentIdAndIsActiveTrue(s.getId()).size())
                .totalTitlesEarned(studentTitleRepository.countByStudentId(s.getId()))
                .build())
                .sorted(Comparator.comparing(SchoolStudentRankResponse::getTotalCoins, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolCampaignReportResponse> getSchoolCampaigns(UUID userId) {
        School school = requireSchool(userId);
        UUID schoolId = school.getId();

        List<SchoolCampaignReportResponse> result = new ArrayList<>();

        List<Campaign> createdCampaigns = campaignRepository.findByCreatorSchoolIdAndIsActiveTrueOrderByCreatedAtDesc(schoolId);
        for (Campaign c : createdCampaigns) {
            Long enrolled = campaignSchoolParticipateRepository.sumStudentsEnrolledByCampaignAndSchool(c.getId(), schoolId);
            Double avgAcc = roundLeaderboardRepository.avgAccuracyByCampaignAndSchool(c.getId(), schoolId);
            result.add(SchoolCampaignReportResponse.builder()
                    .campaignId(c.getId())
                    .campaignCode(c.getCampaignCode())
                    .campaignName(c.getCampaignName())
                    .campaignType(c.getCampaignType())
                    .status(c.getSchoolStatus() != null ? c.getSchoolStatus().name() : "")
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .studentsEnrolled(enrolled != null ? enrolled.intValue() : 0)
                    .avgCombinedAccuracy(safeDouble(avgAcc))
                    .isCreator(true)
                    .build());
        }

        List<CampaignSchoolParticipate> participations = campaignSchoolParticipateRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
        for (CampaignSchoolParticipate csp : participations) {
            Campaign c = csp.getCampaign();
            if (c.getCampaignType() == CampaignType.SCHOOL_INTERNAL || csp.getStatus() != ParticipationStatus.APPROVED) continue;
            Double avgAcc = roundLeaderboardRepository.avgAccuracyByCampaignAndSchool(c.getId(), schoolId);
            result.add(SchoolCampaignReportResponse.builder()
                    .campaignId(c.getId())
                    .campaignCode(c.getCampaignCode())
                    .campaignName(c.getCampaignName())
                    .campaignType(c.getCampaignType())
                    .status(c.getPartnershipStatus() != null ? c.getPartnershipStatus().name() : "")
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .studentsEnrolled(csp.getStudentsEnrolled())
                    .avgCombinedAccuracy(safeDouble(avgAcc))
                    .isCreator(false)
                    .build());
        }

        return result;
    }

    // ── Partnership ───────────────────────────────────────────────────────────

    @Override
    public PartnershipReportSummaryResponse getPartnershipSummary(UUID userId, ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        Partnership partnership = requirePartnership(userId);
        UUID partnershipId = partnership.getId();
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        long totalCreated = campaignRepository.countByCreatorPartnershipId(partnershipId);
        long activeCampaigns = campaignRepository.countByCreatorPartnershipIdAndPartnershipStatusIn(partnershipId,
                List.of(PartnershipCampaignStatus.ON_GOING, PartnershipCampaignStatus.INVITING));
        long completedCampaigns = campaignRepository.countByCreatorPartnershipIdAndPartnershipStatusIn(partnershipId,
                List.of(PartnershipCampaignStatus.COMPLETED));

        long totalSchools = campaignSchoolParticipateRepository.countDistinctSchoolsByPartnershipIdAndStatus(partnershipId, ParticipationStatus.APPROVED);
        Long totalStudents = campaignSchoolParticipateRepository.sumStudentsEnrolledByPartnershipIdAndStatus(partnershipId, ParticipationStatus.APPROVED);
        Double avgAccuracy = roundLeaderboardRepository.avgAccuracyByPartnershipId(partnershipId);

        Subscription activeSub = subscriptionRepository.findByPartnershipIdAndStatus(partnershipId, SubscriptionStatus.ACTIVE).orElse(null);

        List<TopSchoolDto> topSchools = buildTopSchoolsForPartnership(partnershipId);

        return PartnershipReportSummaryResponse.builder()
                .totalCampaignsCreated(totalCreated)
                .activeCampaigns(activeCampaigns)
                .completedCampaigns(completedCampaigns)
                .totalSchoolsParticipated(totalSchools)
                .totalStudentsReached(totalStudents != null ? totalStudents : 0L)
                .avgParticipantAccuracy(safeDouble(avgAccuracy))
                .subscriptionStatus(activeSub != null ? activeSub.getStatus() : null)
                .subscriptionEndDate(activeSub != null ? activeSub.getEndDate() : null)
                .subscriptionPlanName(activeSub != null && activeSub.getPlan() != null ? activeSub.getPlan().getPlanName() : null)
                .topSchoolsByParticipation(topSchools)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    private List<TopSchoolDto> buildTopSchoolsForPartnership(UUID partnershipId) {
        List<Object[]> rows = campaignSchoolParticipateRepository
                .findTopSchoolsByStudentsEnrolledForPartnershipAndStatus(partnershipId, ParticipationStatus.APPROVED, PageRequest.of(0, TOP_SCHOOLS_LIMIT));
        List<TopSchoolDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID schoolId = (UUID) row[0];
            String schoolName = (String) row[1];
            long total = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long campaigns = campaignSchoolParticipateRepository
                    .countBySchoolIdAndPartnershipIdAndStatus(schoolId, partnershipId, ParticipationStatus.APPROVED);
            result.add(TopSchoolDto.builder()
                    .schoolId(schoolId)
                    .schoolName(schoolName)
                    .totalStudentsEnrolled(total)
                    .campaignsParticipated(campaigns)
                    .build());
        }
        return result;
    }

    @Override
    public List<PartnershipCampaignReportResponse> getPartnershipCampaigns(UUID userId) {
        Partnership partnership = requirePartnership(userId);
        UUID partnershipId = partnership.getId();

        List<Campaign> campaigns = campaignRepository.findByCreatorPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(partnershipId);
        return campaigns.stream().map(c -> {
            long schools = campaignSchoolParticipateRepository.countByCampaignIdAndStatus(c.getId(), ParticipationStatus.APPROVED);
            Long students = campaignSchoolParticipateRepository.sumStudentsEnrolledByCampaignIdAndStatus(c.getId(), ParticipationStatus.APPROVED);
            List<RoundLeaderboard> campaignLeaderboard = roundLeaderboardRepository.findByCampaignId(c.getId());
            double avgAcc = campaignLeaderboard.stream()
                    .map(RoundLeaderboard::getCombinedAccuracyPercentage)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0.0);
            return PartnershipCampaignReportResponse.builder()
                    .campaignId(c.getId())
                    .campaignCode(c.getCampaignCode())
                    .campaignName(c.getCampaignName())
                    .status(c.getPartnershipStatus())
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .totalRounds(c.getTotalRounds() != null ? c.getTotalRounds() : 0)
                    .schoolsParticipated(schools)
                    .totalStudentsEnrolled(students != null ? students : 0L)
                    .avgParticipantAccuracy(safeDouble(avgAcc))
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    public AdminReportSummaryResponse getAdminSummary(ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalParents = userRepository.countByRole(Role.PARENT);
        long totalSchools = userRepository.countByRole(Role.PARTNERSHIP_SCHOOL);
        long totalPartnerships = userRepository.countByRole(Role.THIRD_PARTY_PARTNERSHIP);
        long newRegistrations = userRepository.countByCreatedAtBetween(from, to);

        long pendingSchools = schoolRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long pendingPartnerships = partnershipRepository.countByApprovalStatus(ApprovalStatus.PENDING);

        long totalSchoolCampaigns = campaignRepository.countByCampaignType(CampaignType.SCHOOL_INTERNAL);
        long totalPartnershipCampaigns = campaignRepository.countByCampaignType(CampaignType.PARTNERSHIP_EVENT);
        long activeCampaigns = campaignRepository.countAllActiveCampaigns();

        BigDecimal totalRevenue = safeBigDecimal(paymentRepository.sumAmountByStatus(PaymentStatus.COMPLETED));
        BigDecimal revenueInPeriod = safeBigDecimal(paymentRepository.sumAmountByStatusAndDateRange(PaymentStatus.COMPLETED, from, to));
        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        long totalGames = gameSessionRepository.countAllCompleted();
        long totalQuizzes = quizAttemptRepository.countAllCompleted();

        List<MonthlyRevenueTrendDto> trend = buildMonthlyTrend();

        return AdminReportSummaryResponse.builder()
                .totalStudents(totalStudents)
                .totalParents(totalParents)
                .totalSchools(totalSchools)
                .totalPartnerships(totalPartnerships)
                .newRegistrationsInPeriod(newRegistrations)
                .pendingSchools(pendingSchools)
                .pendingPartnerships(pendingPartnerships)
                .totalSchoolCampaigns(totalSchoolCampaigns)
                .totalPartnershipCampaigns(totalPartnershipCampaigns)
                .activeCampaigns(activeCampaigns)
                .totalRevenueAllTime(totalRevenue)
                .totalRevenueInPeriod(revenueInPeriod)
                .activeSubscriptions(activeSubscriptions)
                .totalGameSessionsCompleted(totalGames)
                .totalQuizAttemptsCompleted(totalQuizzes)
                .last12MonthsRevenueTrend(trend)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    @Override
    public AdminRevenueReportResponse getAdminRevenue(ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime[] range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        BigDecimal totalAll = safeBigDecimal(paymentRepository.sumAmountByStatus(PaymentStatus.COMPLETED));
        BigDecimal totalInPeriod = safeBigDecimal(paymentRepository.sumAmountByStatusAndDateRange(PaymentStatus.COMPLETED, from, to));
        BigDecimal fromSchools = safeBigDecimal(paymentRepository.sumAmountBySubscriberType(SubscriberType.SCHOOL));
        BigDecimal fromPartnerships = safeBigDecimal(paymentRepository.sumAmountBySubscriberType(SubscriberType.PARTNERSHIP));

        long totalPayments = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long activeSchoolSubs = subscriptionRepository.countByStatusAndSubscriberType(SubscriptionStatus.ACTIVE, SubscriberType.SCHOOL);
        long activePartnershipSubs = subscriptionRepository.countByStatusAndSubscriberType(SubscriptionStatus.ACTIVE, SubscriberType.PARTNERSHIP);

        List<MonthlyRevenueTrendDto> trend = buildMonthlyTrend();

        return AdminRevenueReportResponse.builder()
                .totalRevenueAllTime(totalAll)
                .totalRevenueInPeriod(totalInPeriod)
                .revenueFromSchools(fromSchools)
                .revenueFromPartnerships(fromPartnerships)
                .totalSuccessfulPayments(totalPayments)
                .activeSubscriptions(activeSubscriptions)
                .activeSchoolSubscriptions(activeSchoolSubs)
                .activePartnershipSubscriptions(activePartnershipSubs)
                .monthlyTrend(trend)
                .period(formatPeriodLabel(period))
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    private List<MonthlyRevenueTrendDto> buildMonthlyTrend() {
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<Object[]> rows = paymentRepository.findMonthlyRevenueTrend(twelveMonthsAgo);
        List<MonthlyRevenueTrendDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal total = row[2] != null ? new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String label = String.format("%02d/%d", month, year);
            result.add(MonthlyRevenueTrendDto.builder().year(year).month(month).totalRevenue(total).label(label).build());
        }
        return result;
    }
}
