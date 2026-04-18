package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.service.ICampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CampaignServiceImpl implements ICampaignService {

    private static final int MAX_PLAYS_PER_LEVEL_PER_DAY = 1000;

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private CampaignRoundRepository campaignRoundRepository;
    @Autowired
    private CampaignParticipantRepository campaignParticipantRepository;
    @Autowired
    private CampaignRoundParticipantRepository campaignRoundParticipantRepository;
    @Autowired
    private CampaignSchoolParticipateRepository campaignSchoolParticipateRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private PartnershipRepository partnershipRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ParentRepository parentRepository;
    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;
    @Autowired
    private RoundGameConfigRepository roundGameConfigRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private RoundLeaderboardRepository roundLeaderboardRepository;
    @Autowired
    private SchoolLeaderboardRepository schoolLeaderboardRepository;
    @Autowired
    private GameTypeRepository gameTypeRepository;
    @Autowired
    private GameLevelPresetRepository gameLevelPresetRepository;
    @Autowired
    private WasteSubCategoryRepository wasteSubCategoryRepository;
    @Autowired
    private DefaultCoinConfigRepository defaultCoinConfigRepository;
    @Autowired
    private CampaignRoundQuizRepository campaignRoundQuizRepository;
    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    @Autowired
    private com.sep490.ecoverse_be.service.ICampaignRewardService campaignRewardService;

    @Autowired
    private com.sep490.ecoverse_be.service.INotificationService notificationService;

    @Autowired
    private com.sep490.ecoverse_be.repository.CampaignTitleRepository campaignTitleRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        return principal.getUser();
    }

    private School getCurrentSchool() {
        return schoolRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));
    }

    private Partnership getCurrentPartnership() {
        return partnershipRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đối tác"));
    }

    private Student getCurrentStudent() {
        return studentRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));
    }

    private Parent getCurrentParent() {
        return parentRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phụ huynh"));
    }

    private String statusOf(Campaign campaign) {
        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            return campaign.getPartnershipStatus().name();
        }
        return campaign.getSchoolStatus().name();
    }

    private void ensureRoundEditableByCurrentUser(Campaign campaign, User user) {
        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            if (campaign.getCreatorPartnership() == null
                    || !campaign.getCreatorPartnership().getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Bạn không có quyền sửa round này");
            }
            return;
        }
        if (campaign.getCreatorSchool() == null
                || !campaign.getCreatorSchool().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Bạn không có quyền sửa round này");
        }
    }

    private Set<WasteCategory> collectAllowedCategories(GameLevelPreset preset) {
        if (preset.getItems() == null) {
            return Set.of();
        }
        return preset.getItems().stream()
                .filter(item -> item.getWasteCategories() != null)
                .flatMap(item -> item.getWasteCategories().stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    private WasteSubCategoryOptionResponse mapWasteSubCategoryOption(WasteSubCategory subCategory) {
        return WasteSubCategoryOptionResponse.builder()
                .id(subCategory.getId())
                .category(subCategory.getCategory())
                .subCategoryCode(subCategory.getSubCategoryCode())
                .displayName(subCategory.getDisplayName())
                .description(subCategory.getDescription())
                .iconUrl(subCategory.getIconUrl())
                .iconPresignedUrl(s3PresignedUrlService.generatePresignedUrl(subCategory.getIconUrl()))
                .displayOrder(subCategory.getDisplayOrder())
                .build();
    }

    private String createCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ── Subscription quota helpers ─────────────────────────────────────────────

    private Subscription getActiveSchoolSubscription(School school) {
        return subscriptionRepository.findBySchoolIdAndStatus(school.getId(), com.sep490.ecoverse_be.enums.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Trường chưa có gói subscription hoạt động"));
    }

    private Subscription getActivePartnershipSubscription(Partnership partnership) {
        return subscriptionRepository.findByPartnershipIdAndStatus(partnership.getId(), com.sep490.ecoverse_be.enums.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Tổ chức chưa có gói subscription hoạt động"));
    }

    private void checkCampaignPerMonthQuota(Subscription subscription, School school, Partnership partnership) {
        Integer maxCampaignsPerMonth = subscription.getPlan().getMaxCampaignsPerMonth();
        if (maxCampaignsPerMonth == null) return; // unlimited

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long usedCount;
        if (school != null) {
            usedCount = campaignRepository.countNonDraftByCreatorSchoolIdInMonth(school.getId(), startOfMonth);
        } else {
            usedCount = campaignRepository.countNonDraftByCreatorPartnershipIdInMonth(partnership.getId(), startOfMonth);
        }

        if (usedCount >= maxCampaignsPerMonth) {
            throw new BadRequestException(String.format(
                    "Đã đạt giới hạn số chiến dịch trong tháng (%d/%d). Không thể kích hoạt thêm chiến dịch.",
                    usedCount, maxCampaignsPerMonth));
        }
    }

    private void checkMaxRoundsPerCampaign(Subscription subscription, int requestedRounds) {
        Integer maxRoundsPerCampaign = subscription.getPlan().getMaxRoundsPerCampaign();
        if (maxRoundsPerCampaign == null) return; // unlimited

        if (requestedRounds > maxRoundsPerCampaign) {
            throw new BadRequestException(String.format(
                    "Số round (%d) vượt quá giới hạn của gói subscription (%d round/campaign).",
                    requestedRounds, maxRoundsPerCampaign));
        }
    }

    private void checkMaxSchoolsPerCampaign(Subscription subscription, UUID campaignId, int additionalSchools) {
        Integer maxSchoolsPerCampaign = subscription.getPlan().getMaxSchoolsPerCampaign();
        if (maxSchoolsPerCampaign == null) return; // unlimited

        long currentSchools = campaignSchoolParticipateRepository.findByCampaignId(campaignId).size();
        if (currentSchools + additionalSchools > maxSchoolsPerCampaign) {
            throw new BadRequestException(String.format(
                    "Tổng số trường mời (%d) vượt quá giới hạn của gói subscription (%d trường/campaign).",
                    currentSchools + additionalSchools, maxSchoolsPerCampaign));
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BadRequestException("Thời gian bắt đầu/kết thúc không hợp lệ");
        }
    }

    // Tự động tạo 5 danh hiệu mặc định khi tạo campaign mới
    private void createDefaultTitles(Campaign campaign, School school, Partnership partnership) {
        User creator = getCurrentUser();
        record TitleDef(TitleCriteriaType type, String name, String displayFormat, String description) {}
        List<TitleDef> defs = List.of(
                new TitleDef(TitleCriteriaType.FASTEST_COMPLETION,
                        "Hoàn thành nhanh nhất",
                        "Người hoàn thành nhanh nhất - " + campaign.getCampaignName(),
                        "Học sinh có tổng thời gian hoàn thành thấp nhất trong chiến dịch"),
                new TitleDef(TitleCriteriaType.HIGHEST_ACCURACY,
                        "Độ chính xác cao nhất",
                        "Người có độ chính xác cao nhất - " + campaign.getCampaignName(),
                        "Học sinh đạt độ chính xác cao nhất qua các vòng thi"),
                new TitleDef(TitleCriteriaType.BEST_ACCURACY_AND_TIME,
                        "Hiệu suất tổng thể tốt nhất",
                        "Người có hiệu suất tổng thể tốt nhất - " + campaign.getCampaignName(),
                        "Học sinh kết hợp tốt nhất giữa độ chính xác và tốc độ"),
                new TitleDef(TitleCriteriaType.MOST_GAMES_COMPLETED,
                        "Hoàn thành nhiều game nhất",
                        "Người hoàn thành nhiều game nhất - " + campaign.getCampaignName(),
                        "Học sinh hoàn thành nhiều lượt chơi game nhất trong chiến dịch"),
                new TitleDef(TitleCriteriaType.MOST_QUIZZES_PASSED,
                        "Vượt qua nhiều quiz nhất",
                        "Người vượt qua nhiều quiz nhất - " + campaign.getCampaignName(),
                        "Học sinh hoàn thành nhiều bài quiz nhất trong chiến dịch")
        );

        for (TitleDef def : defs) {
            CampaignTitle title = new CampaignTitle();
            title.setCampaign(campaign);
            title.setSchool(school);
            title.setPartnership(partnership);
            title.setTitleName(def.name());
            title.setDisplayFormat(def.displayFormat());
            title.setDescription(def.description());
            title.setCriteriaType(def.type());
            title.setMaxRecipients(1);
            title.setActive(true);
            title.setCreatedBy(creator);
            campaignTitleRepository.save(title);
        }
    }

    private Campaign getSchoolCampaignOwned(UUID campaignId, UUID schoolId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        if (campaign.getCreatorSchool() == null || !campaign.getCreatorSchool().getId().equals(schoolId)) {
            throw new BadRequestException("Bạn không có quyền truy cập campaign này");
        }
        return campaign;
    }

    private Campaign getPartnershipCampaignOwned(UUID campaignId, UUID partnershipId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        if (campaign.getCreatorPartnership() == null
                || !campaign.getCreatorPartnership().getId().equals(partnershipId)) {
            throw new BadRequestException("Bạn không có quyền truy cập campaign này");
        }
        return campaign;
    }

    private CampaignDetailResponse mapCampaignDetail(Campaign campaign) {
        return mapCampaignDetail(campaign, null);
    }

    private CampaignDetailResponse mapCampaignDetail(Campaign campaign, UUID currentParticipantId) {
        List<CampaignRoundInfoResponse> rounds = campaignRoundRepository
                .findByCampaignIdOrderByRoundNumberAsc(campaign.getId())
                .stream()
                .map(r -> {
                    Optional<RoundGameConfig> configOpt = roundGameConfigRepository
                            .findFirstByCampaignRoundIdOrderByDisplayOrderAsc(r.getId());

                    UUID gameTypeId = configOpt.map(cfg -> cfg.getGameType().getId()).orElse(null);
                    String gameTypeName = configOpt.map(cfg -> cfg.getGameType().getName()).orElse(null);
                    List<UUID> selectedPresetIds = configOpt
                            .map(cfg -> cfg.getSelectedPresets() == null
                                    ? List.<UUID>of()
                                    : cfg.getSelectedPresets().stream().map(BaseEntity::getId).toList())
                            .orElse(List.of());
                    Map<String, List<UUID>> presetSubCategoryConfig = configOpt
                            .map(cfg -> cfg.getPresetSubCategoryConfig() == null
                                    ? Map.<String, List<UUID>>of()
                                    : cfg.getPresetSubCategoryConfig())
                            .orElse(Map.of());

                    List<StudentRoundGameConfigResponse> games = roundGameConfigRepository
                            .findByCampaignRoundIdOrderByDisplayOrderAsc(r.getId())
                            .stream()
                            .map(config -> {
                                Integer coinPerSession = null;
                                if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
                                    if (config.getCoinPerSession() != null) {
                                        coinPerSession = config.getCoinPerSession();
                                    } else if (config.getResolvedDifficulty() != null) {
                                        coinPerSession = defaultCoinConfigRepository
                                                .findByGameTypeIdAndDifficulty(config.getGameType().getId(),
                                                        config.getResolvedDifficulty())
                                                .map(DefaultCoinConfig::getDefaultCoin)
                                                .orElse(0);
                                    }
                                }

                                Map<String, List<UUID>> presetSubCategoryConfigGame = config.getPresetSubCategoryConfig() == null
                                        ? Map.of()
                                        : config.getPresetSubCategoryConfig();

                                List<StudentRoundPresetConfigResponse> presets = config.getSelectedPresets() == null
                                        ? List.of()
                                        : config.getSelectedPresets().stream()
                                                .map(preset -> {
                                                    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
                                                    LocalDateTime endOfDay = startOfDay.plusDays(1);

                                                    List<StudentPresetLevelConfigResponse> items = preset.getItems() == null
                                                            ? List.of()
                                                            : preset.getItems().stream()
                                                                    .map(item -> {
                                                                        long todayAttempts = 0;
                                                                        Boolean coinReceived = null;
                                                                        Boolean isPassed = null;

                                                                        if (currentParticipantId != null) {
                                                                            todayAttempts = gameSessionRepository
                                                                                    .countByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndSessionStartBetween(
                                                                                            currentParticipantId,
                                                                                            config.getId(),
                                                                                            preset.getId(),
                                                                                            item.getLevelNumber(),
                                                                                            startOfDay,
                                                                                            endOfDay);
                                                                            if (campaign.getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
                                                                                coinReceived = gameSessionRepository
                                                                                            .existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndCoinAwardedGreaterThan(
                                                                                                    currentParticipantId,
                                                                                                    config.getId(),
                                                                                                    preset.getId(),
                                                                                                    item.getLevelNumber(),
                                                                                                    0);
                                                                            }
                                                                            isPassed = gameSessionRepository
                                                                                    .existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndIsCompletedTrueAndIsPassedTrue(
                                                                                            currentParticipantId,
                                                                                            config.getId(),
                                                                                            preset.getId(),
                                                                                            item.getLevelNumber());
                                                                        }

                                                                        return StudentPresetLevelConfigResponse.builder()
                                                                                .levelNumber(item.getLevelNumber())
                                                                                .itemCount(item.getItemCount())
                                                                                .timeLimitSeconds(item.getTimeLimitSeconds())
                                                                                .scorePerCorrect(item.getScorePerCorrect())
                                                                                .lives(item.getLives())
                                                                                .wasteCategories(
                                                                                        item.getWasteCategories() == null ? Set.of()
                                                                                                : item.getWasteCategories())
                                                                                .configJson(item.getConfigJson() == null ? Map.of()
                                                                                        : item.getConfigJson())
                                                                                .coinReceived(coinReceived)
                                                                                .maxDailyAttempts(MAX_PLAYS_PER_LEVEL_PER_DAY)
                                                                                .todayAttempts(todayAttempts)
                                                                                .isPassed(isPassed)
                                                                                .build();
                                                                    })
                                                                    .toList();

                                                    List<UUID> configuredSubCategoryIds = presetSubCategoryConfigGame.getOrDefault(
                                                            preset.getId().toString(),
                                                            List.of());

                                                    return StudentRoundPresetConfigResponse.builder()
                                                            .presetId(preset.getId())
                                                            .difficulty(preset.getDifficulty())
                                                            .configuredSubCategoryIds(configuredSubCategoryIds)
                                                            .items(items)
                                                            .build();
                                                })
                                                .toList();

                                return StudentRoundGameConfigResponse.builder()
                                        .roundGameConfigId(config.getId())
                                        .gameTypeId(config.getGameType().getId())
                                        .typeCode(config.getGameType().getTypeCode().name())
                                        .gameTypeName(config.getGameType().getName())
                                        .resolvedDifficulty(config.getResolvedDifficulty())
                                        .coinPerSession(coinPerSession)
                                        .presets(presets)
                                        .build();
                            })
                            .toList();

                    // Lấy danh sách quiz kèm thông tin chi tiết từ CampaignRoundQuiz
                    List<RoundQuizBriefResponse> quizzes = campaignRoundQuizRepository
                            .findByCampaignRoundIdOrderByDisplayOrderAsc(r.getId())
                            .stream()
                            .map(rq -> {
                                int attemptsUsed = 0;
                                boolean isPassed = false;
                                if (currentParticipantId != null) {
                                    attemptsUsed = quizAttemptRepository.countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
                                            currentParticipantId, r.getId(), rq.getQuiz().getId());
                                    isPassed = quizAttemptRepository
                                            .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                                                    currentParticipantId, r.getId(), rq.getQuiz().getId())
                                            .map(QuizAttempt::isPassed)
                                            .orElse(false);
                                }
                                return RoundQuizBriefResponse.builder()
                                        .quizId(rq.getQuiz().getId())
                                        .title(rq.getQuiz().getTitle())
                                        .difficulty(rq.getQuiz().getDifficulty())
                                        .displayOrder(rq.getDisplayOrder())
                                        .attemptsUsed(attemptsUsed)
                                        .isPassed(isPassed)
                                        .maxAttempts(rq.getMaxAttempts())
                                        .isRequired(rq.isRequired())
                                        .build();
                            })
                            .toList();

                    return CampaignRoundInfoResponse.builder()
                            .id(r.getId())
                            .roundNumber(r.getRoundNumber())
                            .roundName(r.getRoundName())
                            .status(r.getStatus())
                            .startTime(r.getStartTime())
                            .endTime(r.getEndTime())
                            .maxParticipants(r.getMaxParticipants())
                            .advanceCount(r.getAdvanceCount())
                            .isFinalRound(r.getIsFinalRound())
                            .gameTypeId(gameTypeId)
                            .gameTypeName(gameTypeName)
                            .coinPerSession(configOpt.map(RoundGameConfig::getCoinPerSession).orElse(null))
                            .selectedPresetIds(selectedPresetIds)
                            .presetSubCategoryConfig(presetSubCategoryConfig)
                            .games(games)
                            .quizzes(quizzes)
                            .build();
                })
                .toList();

        // Lấy tất cả học sinh đã được mời (không lọc isActive) để UI hiển thị đầy đủ
        // trạng thái
        List<CampaignParticipantInfoResponse> participants = campaignParticipantRepository
                .findByCampaignIdOrderByCreatedAtAsc(campaign.getId())
                .stream()
                .map(p -> CampaignParticipantInfoResponse.builder()
                        .studentId(p.getStudent().getId())
                        .studentCode(p.getStudent().getStudentCode())
                        .fullName(p.getStudent().getFullName())
                        .gradeLevel(p.getStudent().getGradeLevel())
                        .className(p.getStudent().getClassName())
                        .parentApprovalStatus(p.getParentApprovalStatus())
                        .invitationSentAt(p.getInvitationSentAt())
                        .rejectionReason(p.getRejectionReason())
                        .build())
                .toList();

        // Lấy danh sách trường đã được mời (chỉ có với PARTNERSHIP_EVENT)
        List<InvitedSchoolInfoResponse> invitedSchools = null;
        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            invitedSchools = campaignSchoolParticipateRepository.findByCampaignId(campaign.getId())
                    .stream()
                    .map(sp -> InvitedSchoolInfoResponse.builder()
                            .invitationId(sp.getId())
                            .schoolId(sp.getSchool().getId())
                            .schoolName(sp.getSchool().getSchoolName())
                            .status(sp.getStatus())
                            .studentsEnrolled(sp.getStudentsEnrolled())
                            .invitationSentAt(sp.getInvitationSentAt())
                            .participationConfirmedAt(sp.getParticipationConfirmedAt())
                            .build())
                    .toList();
        }

        return CampaignDetailResponse.builder()
                .id(campaign.getId())
                .campaignCode(campaign.getCampaignCode())
                .campaignName(campaign.getCampaignName())
                .campaignType(campaign.getCampaignType())
                .description(campaign.getDescription())
                .status(statusOf(campaign))
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .registrationDate(campaign.getRegistrationDate())
                .registrationDeadline(campaign.getRegistrationDeadline())
                .invitationDate(campaign.getInvitationDate())
                .invitationDeadline(campaign.getInvitationDeadline())
                .maxStudentsPerSchool(campaign.getMaxStudentsPerSchool())
                .totalStudentQuota(campaign.getTotalStudentQuota())
                .topRankingCount(campaign.getTopRankingCount())
                .totalRounds(campaign.getTotalRounds())
                .bannerImageUrl(campaign.getBannerImageUrl())
                .bannerImagePresignedUrl(s3PresignedUrlService.generatePresignedUrl(campaign.getBannerImageUrl()))
                .rounds(rounds)
                .participants(participants)
                .invitedSchools(invitedSchools)
                .build();
    }

    /** Dùng cho danh sách campaign phía trường/đối tác — không có trạng thái tham gia của học sinh. */
    private CampaignSummaryResponse mapCampaignSummary(Campaign campaign) {
        return mapCampaignSummary(campaign, null);
    }

    private CampaignSummaryResponse mapCampaignSummary(Campaign campaign, ParticipationStatus participationStatus) {
        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());

        boolean hasQuiz = rounds.stream()
                .anyMatch(r -> campaignRoundQuizRepository.countByCampaignRoundId(r.getId()) > 0);

        boolean hasGame = rounds.stream()
                .anyMatch(r -> roundGameConfigRepository.findFirstByCampaignRoundIdOrderByDisplayOrderAsc(r.getId())
                        .map(cfg -> cfg.getSelectedPresets() != null && !cfg.getSelectedPresets().isEmpty())
                        .orElse(false));

        return CampaignSummaryResponse.builder()
                .id(campaign.getId())
                .campaignCode(campaign.getCampaignCode())
                .campaignName(campaign.getCampaignName())
                .campaignType(campaign.getCampaignType())
                .status(statusOf(campaign))
                .participationStatus(participationStatus)
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .registrationDate(campaign.getRegistrationDate())
                .registrationDateDeadline(campaign.getRegistrationDeadline())
                .invitationDate(campaign.getInvitationDate())
                .invitationDeadline(campaign.getInvitationDeadline())
                .description(campaign.getDescription())
                .hasQuiz(hasQuiz)
                .hasGame(hasGame)
                .build();
    }

    private void ensureHasAtLeastOneGameAndQuiz(Campaign campaign) {
        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());
        boolean hasAtLeastOneQuiz = rounds.stream().anyMatch(round -> round.getQuiz() != null
                || (round.getSelectedQuizzes() != null && !round.getSelectedQuizzes().isEmpty()));
        boolean hasAtLeastOneGame = rounds.stream().anyMatch(round -> roundGameConfigRepository
                .findFirstByCampaignRoundIdOrderByDisplayOrderAsc(round.getId())
                .map(cfg -> cfg.getSelectedPresets() != null && !cfg.getSelectedPresets().isEmpty())
                .orElse(false));

        if (!hasAtLeastOneGame || !hasAtLeastOneQuiz) {
            throw new BadRequestException(
                    "Campaign cần add ít nhất 1 game preset và 1 quiz trước khi chuyển khỏi DRAFT");
        }
    }

    private Map<UUID, List<UUID>> normalizePresetSubCategoryConfigs(UpdateRoundGameConfigRequest request) {
        if (request.getPresetSubCategoryConfigs() == null || request.getPresetSubCategoryConfigs().isEmpty()) {
            throw new BadRequestException("Cần cấu hình sub-category cho từng preset");
        }
        Map<UUID, List<UUID>> normalized = new LinkedHashMap<>();
        for (RoundPresetSubCategoryConfigRequest cfg : request.getPresetSubCategoryConfigs()) {
            if (normalized.containsKey(cfg.getPresetId())) {
                throw new BadRequestException("presetSubCategoryConfigs chứa preset bị trùng");
            }
            LinkedHashSet<UUID> uniqueSubCategoryIds = new LinkedHashSet<>(cfg.getSelectedSubCategoryIds());
            if (uniqueSubCategoryIds.isEmpty()) {
                throw new BadRequestException("Mỗi preset phải chọn ít nhất 1 sub-category");
            }
            normalized.put(cfg.getPresetId(), new ArrayList<>(uniqueSubCategoryIds));
        }
        return normalized;
    }

    @Override
    public List<PresetAvailableSubCategoriesResponse> getAvailableSubCategoriesForPresets(UUID roundId, UUID gameTypeId,
            List<UUID> presetIds) {
        User user = getCurrentUser();
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        ensureRoundEditableByCurrentUser(round.getCampaign(), user);

        if (presetIds == null || presetIds.isEmpty()) {
            throw new BadRequestException("Cần cung cấp ít nhất 1 preset");
        }

        GameType gameType = gameTypeRepository.findById(gameTypeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy game type"));

        List<UUID> orderedPresetIds = new ArrayList<>(new LinkedHashSet<>(presetIds));
        List<GameLevelPreset> presets = gameLevelPresetRepository.findByIdInAndGameTypeId(orderedPresetIds,
                gameType.getId());
        if (presets.size() != orderedPresetIds.size()) {
            throw new BadRequestException("presetIds chứa preset không hợp lệ");
        }

        Map<UUID, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < orderedPresetIds.size(); i++) {
            orderMap.put(orderedPresetIds.get(i), i);
        }
        presets = presets.stream()
                .sorted(Comparator.comparingInt(p -> orderMap.getOrDefault(p.getId(), Integer.MAX_VALUE)))
                .toList();

        Set<WasteCategory> allCategories = presets.stream()
                .map(this::collectAllowedCategories)
                .flatMap(Set::stream)
                .collect(java.util.stream.Collectors.toSet());

        List<WasteSubCategory> activeSubCategories = allCategories.isEmpty()
                ? List.of()
                : wasteSubCategoryRepository.findByCategoryInAndIsDeleteFalse(new ArrayList<>(allCategories)).stream()
                        .sorted(Comparator.comparingInt(WasteSubCategory::getDisplayOrder)
                                .thenComparing(WasteSubCategory::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                        .toList();

        return presets.stream()
                .map(preset -> {
                    Set<WasteCategory> allowedCategories = collectAllowedCategories(preset);
                    List<WasteSubCategoryOptionResponse> options = activeSubCategories.stream()
                            .filter(subCategory -> allowedCategories.contains(subCategory.getCategory()))
                            .collect(java.util.stream.Collectors.collectingAndThen(
                                    java.util.stream.Collectors.toMap(
                                            WasteSubCategory::getId,
                                            this::mapWasteSubCategoryOption,
                                            (left, right) -> left,
                                            LinkedHashMap::new),
                                    map -> new ArrayList<>(map.values())));

                    return PresetAvailableSubCategoriesResponse.builder()
                            .presetId(preset.getId())
                            .gameTypeId(gameType.getId())
                            .difficulty(preset.getDifficulty())
                            .availableSubCategories(options)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public CampaignDetailResponse createSchoolCampaign(SchoolCampaignUpsertRequest request) {
        School school = getCurrentSchool();
        validateDateRange(request.getStartDate(), request.getEndDate());

        Campaign campaign = new Campaign();
        campaign.setCampaignCode(createCode("SCH"));
        campaign.setCampaignName(request.getCampaignName());
        campaign.setCampaignType(CampaignType.SCHOOL_INTERNAL);
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setTopRankingCount(request.getTopRankingCount() != null ? request.getTopRankingCount() : 10);
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaign.setTotalRounds(1);
        campaign.setCreatorSchool(school);
        campaign.setCreatedBy(getCurrentUser());
        campaign.setSchoolStatus(SchoolCampaignStatus.DRAFT);
        campaign = campaignRepository.save(campaign);

        CampaignRound round = new CampaignRound();
        round.setCampaign(campaign);
        round.setRoundNumber(1);
        round.setRoundName("Play & Learn");
        round.setStartTime(campaign.getStartDate());
        round.setEndTime(campaign.getEndDate());
        campaignRoundRepository.save(round);

        // Mời học sinh ngay khi tạo campaign nếu có danh sách studentIds
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            List<Student> students = studentRepository.findAllById(request.getStudentIds());
            for (Student student : students) {
                if (!student.getSchool().getId().equals(school.getId())) {
                    continue;
                }
                CampaignParticipant participant = new CampaignParticipant();
                participant.setCampaign(campaign);
                participant.setStudent(student);
                participant.setSchool(school);
                participant.setEnrollmentDate(LocalDateTime.now());
                participant.setParentApprovalStatus(ParticipationStatus.PREPARED);
                campaignParticipantRepository.save(participant);
            }
        }

        createDefaultTitles(campaign, school, null);

        return mapCampaignDetail(campaign);
    }

    @Override
    public List<CampaignSummaryResponse> getMySchoolCampaigns() {
        School school = getCurrentSchool();
        return campaignRepository.findByCreatorSchoolIdAndIsActiveTrueOrderByCreatedAtDesc(school.getId())
                .stream()
                .map(this::mapCampaignSummary)
                .toList();
    }

    @Override
    public CampaignDetailResponse getSchoolCampaignById(UUID campaignId) {
        return mapCampaignDetail(getSchoolCampaignOwned(campaignId, getCurrentSchool().getId()));
    }

    @Override
    @Transactional
    public CampaignDetailResponse updateSchoolCampaign(UUID campaignId, SchoolCampaignUpsertRequest request) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được sửa campaign ở trạng thái DRAFT");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());
        campaign.setCampaignName(request.getCampaignName());
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setTopRankingCount(
                request.getTopRankingCount() != null ? request.getTopRankingCount() : campaign.getTopRankingCount());
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaignRepository.save(campaign);

        CampaignRound round = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId())
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        round.setStartTime(request.getStartDate());
        round.setEndTime(request.getEndDate());
        campaignRoundRepository.save(round);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse activateSchoolCampaign(UUID campaignId) {
        School school = getCurrentSchool();
        Campaign campaign = getSchoolCampaignOwned(campaignId, school.getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được kích hoạt campaign ở trạng thái DRAFT");
        }
        // Check campaign per month quota
        Subscription schoolSub = getActiveSchoolSubscription(school);
        checkCampaignPerMonthQuota(schoolSub, school, null);

        ensureHasAtLeastOneGameAndQuiz(campaign);
        campaign.setSchoolStatus(SchoolCampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);

        // Tự động tạo bản ghi tham gia cho chính trường tạo campaign (school tự mời
        // mình)
        boolean alreadyParticipating = campaignSchoolParticipateRepository
                .findByCampaignIdAndSchoolId(campaignId, school.getId()).isPresent();
        if (!alreadyParticipating) {
            CampaignSchoolParticipate selfParticipate = new CampaignSchoolParticipate();
            selfParticipate.setCampaign(campaign);
            selfParticipate.setSchool(school);
            selfParticipate.setStatus(ParticipationStatus.APPROVED);
            selfParticipate.setParticipationConfirmedAt(LocalDateTime.now());
            campaignSchoolParticipateRepository.save(selfParticipate);
        }

        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse setSchoolCampaignDraft(UUID campaignId) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.SCHEDULED) {
            throw new BadRequestException("Chỉ được chuyển về DRAFT khi campaign ở trạng thái SCHEDULED");
        }
        campaign.setSchoolStatus(SchoolCampaignStatus.DRAFT);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse extendInviting(UUID campaignId, ExtendInvitingRequest request) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.INVITING) {
            throw new BadRequestException("Chỉ được gia hạn khi campaign đang INVITING");
        }
        if (!request.getNewInviteEndAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Thời gian gia hạn phải lớn hơn hiện tại");
        }
        campaign.setInvitationDeadline(request.getNewInviteEndAt());
        campaign.setSchoolStatus(SchoolCampaignStatus.EXTENDED);
        campaignRepository.save(campaign);

        if (request.getAdditionalStudentIds() != null && !request.getAdditionalStudentIds().isEmpty()) {
            School school = getCurrentSchool();
            List<Student> students = studentRepository.findAllById(request.getAdditionalStudentIds());
            for (Student student : students) {
                if (!student.getSchool().getId().equals(school.getId())) {
                    continue;
                }
                if (campaignParticipantRepository.existsByCampaignIdAndStudentId(campaignId, student.getId())) {
                    continue;
                }
                CampaignParticipant participant = new CampaignParticipant();
                participant.setCampaign(campaign);
                participant.setSchool(school);
                participant.setStudent(student);
                participant.setEnrollmentDate(LocalDateTime.now());
                participant.setParentApprovalStatus(ParticipationStatus.PREPARED);
                participant.setInvitationSentAt(LocalDateTime.now());
                campaignParticipantRepository.save(participant);
            }
        }
        return mapCampaignDetail(campaign);
    }

    private static final String CAMPAIGN_CANCELLED_INVITATION_REASON = "Chiến dịch đã bị hủy bởi người tổ chức.";

    /**
     * Gửi trước khi invalidate vì notifyCampaignParents/notifyCampaignParticipants
     * chỉ lấy participant isActive=true.
     */
    private void notifyStakeholdersOfCampaignCancellation(Campaign campaign) {
        UUID campaignId = campaign.getId();
        String name = campaign.getCampaignName();
        String title = "Chiến dịch đã bị hủy";
        String message = "Chiến dịch \"" + name + "\" đã bị hủy. " + CAMPAIGN_CANCELLED_INVITATION_REASON;

        notificationService.notifyCampaignParents(
                campaignId,
                NotificationType.CAMPAIGN_CANCELLED,
                title,
                message,
                "campaign",
                campaignId,
                null);
        notificationService.notifyCampaignParticipants(
                campaignId,
                NotificationType.CAMPAIGN_CANCELLED,
                title,
                message,
                "campaign",
                campaignId,
                null);

        List<User> schoolAccountUsers = campaignSchoolParticipateRepository.findByCampaignId(campaignId).stream()
                .map(sp -> sp.getSchool().getUser())
                .distinct()
                .toList();
        if (!schoolAccountUsers.isEmpty()) {
            notificationService.notifyUsers(
                    schoolAccountUsers,
                    NotificationType.CAMPAIGN_CANCELLED,
                    title,
                    message,
                    "campaign",
                    campaignId,
                    null,
                    false);
        }
    }

    private void invalidateInvitationsOnCampaignCancel(Campaign campaign) {
        String reason = CAMPAIGN_CANCELLED_INVITATION_REASON;
        for (CampaignSchoolParticipate sp : campaignSchoolParticipateRepository.findByCampaignId(campaign.getId())) {
            if (sp.getStatus() == ParticipationStatus.CANCELLED) {
                continue;
            }
            sp.setStatus(ParticipationStatus.CANCELLED);
            sp.setRejectionReason(reason);
            campaignSchoolParticipateRepository.save(sp);
        }
        for (CampaignParticipant p : campaignParticipantRepository
                .findByCampaignIdOrderByCreatedAtAsc(campaign.getId())) {
            if (p.getParentApprovalStatus() == ParticipationStatus.CANCELLED) {
                continue;
            }
            p.setParentApprovalStatus(ParticipationStatus.CANCELLED);
            p.setRejectionReason(reason);
            p.setActive(false);
            campaignParticipantRepository.save(p);
        }
    }

    @Override
    @Transactional
    public CampaignDetailResponse cancelSchoolCampaign(UUID campaignId) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.EXTENDED) {
            throw new BadRequestException("Chỉ được hủy campaign ở trạng thái EXTENDED");
        }
        campaign.setSchoolStatus(SchoolCampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        notifyStakeholdersOfCampaignCancellation(campaign);
        invalidateInvitationsOnCampaignCancel(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public void deleteSchoolCampaign(UUID campaignId) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được xóa campaign ở trạng thái DRAFT");
        }
        campaign.setActive(false);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void inviteStudentsToSchoolCampaign(UUID campaignId, AssignStudentsRequest request) {
        School school = getCurrentSchool();
        Campaign campaign = getSchoolCampaignOwned(campaignId, school.getId());

        // Cho phép chọn học sinh ở trạng thái DRAFT hoặc SCHEDULED (trước khi scheduler
        // chuyển sang INVITING)
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT
                && campaign.getSchoolStatus() != SchoolCampaignStatus.SCHEDULED) {
            throw new BadRequestException("Chỉ được mời học sinh khi campaign đang ở trạng thái DRAFT hoặc SCHEDULED");
        }

        List<Student> students = studentRepository.findAllById(request.getStudentIds());
        int added = 0;
        for (Student student : students) {
            if (!student.getSchool().getId().equals(school.getId())) {
                continue;
            }
            if (campaignParticipantRepository.existsByCampaignIdAndStudentId(campaignId, student.getId())) {
                continue;
            }
            CampaignParticipant participant = new CampaignParticipant();
            participant.setCampaign(campaign);
            participant.setStudent(student);
            participant.setSchool(school);
            participant.setEnrollmentDate(LocalDateTime.now());
            participant.setParentApprovalStatus(ParticipationStatus.PREPARED);
            campaignParticipantRepository.save(participant);
            added++;
        }

        // Cập nhật studentsEnrolled trên CampaignSchoolParticipate nếu đã tồn tại
        // Dùng final variable để dùng được trong lambda
        final int totalAdded = added;
        campaignSchoolParticipateRepository.findByCampaignIdAndSchoolId(campaignId, school.getId())
                .ifPresent(sp -> {
                    sp.setStudentsEnrolled(sp.getStudentsEnrolled() + totalAdded);
                    campaignSchoolParticipateRepository.save(sp);
                });
    }

    @Override
    public List<EligibleSchoolResponse> getEligibleSchools() {
        Partnership partnership = getCurrentPartnership();
        List<School> schools;
        if (partnership.getGeographicScopeWard() != null && !partnership.getGeographicScopeWard().isBlank()) {
            schools = schoolRepository.findByWardAndApprovalStatus(
                    partnership.getGeographicScopeWard(), ApprovalStatus.APPROVED);
        } else if (partnership.getGeographicScopeProvince() != null
                && !partnership.getGeographicScopeProvince().isBlank()) {
            schools = schoolRepository.findByProvinceAndApprovalStatus(
                    partnership.getGeographicScopeProvince(), ApprovalStatus.APPROVED);
        } else {
            schools = List.of();
        }
        return schools.stream()
                .map(s -> EligibleSchoolResponse.builder()
                        .schoolId(s.getId())
                        .schoolName(s.getSchoolName())
                        .ward(s.getWard())
                        .province(s.getProvince())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public CampaignDetailResponse createPartnershipCampaign(CreatePartnershipCampaignRequest request) {
        Partnership partnership = getCurrentPartnership();
        validateDateRange(request.getStartDate(), request.getEndDate());
        if (request.getRounds() == null || request.getRounds().isEmpty()) {
            throw new BadRequestException("Partnership campaign phải có ít nhất 1 round");
        }

        // Check maxRoundsPerCampaign quota
        Subscription partnershipSub = getActivePartnershipSubscription(partnership);
        checkMaxRoundsPerCampaign(partnershipSub, request.getRounds().size());

        // Check maxSchoolsPerCampaign quota (at creation time if schoolIds are provided)
        if (request.getSchoolIds() != null && !request.getSchoolIds().isEmpty()) {
            Integer maxSchools = partnershipSub.getPlan().getMaxSchoolsPerCampaign();
            if (maxSchools != null && request.getSchoolIds().size() > maxSchools) {
                throw new BadRequestException(String.format(
                        "Số trường mời (%d) vượt quá giới hạn của gói subscription (%d trường/campaign).",
                        request.getSchoolIds().size(), maxSchools));
            }
        }

        Campaign campaign = new Campaign();
        campaign.setCampaignCode(createCode("PRT"));
        campaign.setCampaignName(request.getCampaignName());
        campaign.setCampaignType(CampaignType.PARTNERSHIP_EVENT);
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setRegistrationDate(request.getRegistrationDate());
        campaign.setRegistrationDeadline(request.getRegistrationDeadline());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setMaxStudentsPerSchool(request.getMaxStudentsPerSchool());
        campaign.setTotalStudentQuota(request.getTotalStudentQuota());
        campaign.setTopRankingCount(request.getTopRankingCount() != null ? request.getTopRankingCount() : 10);
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaign.setTotalRounds(request.getRounds().size());
        campaign.setCreatorPartnership(partnership);
        campaign.setCreatedBy(getCurrentUser());
        campaign.setPartnershipStatus(PartnershipCampaignStatus.DRAFT);
        campaign = campaignRepository.save(campaign);

        for (PartnershipRoundRequest roundRequest : request.getRounds()) {
            if (!roundRequest.getEndTime().isAfter(roundRequest.getStartTime())) {
                throw new BadRequestException("Thời gian round không hợp lệ");
            }
            CampaignRound round = new CampaignRound();
            round.setCampaign(campaign);
            round.setRoundNumber(roundRequest.getRoundNumber());
            round.setRoundName(roundRequest.getRoundName());
            round.setStartTime(roundRequest.getStartTime());
            round.setEndTime(roundRequest.getEndTime());
            round.setMaxParticipants(roundRequest.getMaxParticipants());
            round.setAdvanceCount(roundRequest.getAdvanceCount());
            round.setIsFinalRound(Boolean.TRUE.equals(roundRequest.getIsFinalRound()));
            campaignRoundRepository.save(round);
        }

        if (request.getSchoolIds() != null && !request.getSchoolIds().isEmpty()) {
            final Campaign savedCampaign = campaign;
            List<School> schools = schoolRepository.findAllById(request.getSchoolIds());
            for (School school : schools) {
                CampaignSchoolParticipate invitation = new CampaignSchoolParticipate();
                invitation.setCampaign(savedCampaign);
                invitation.setSchool(school);
                invitation.setStatus(ParticipationStatus.PREPARED);
                campaignSchoolParticipateRepository.save(invitation);
            }
        }

        campaignRewardService.saveRewards(campaign, partnership, request.getRewards());

        createDefaultTitles(campaign, null, partnership);

        return mapCampaignDetail(campaign);
    }

    @Override
    public List<CampaignSummaryResponse> getMyPartnershipCampaigns() {
        Partnership partnership = getCurrentPartnership();
        return campaignRepository.findByCreatorPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(partnership.getId())
                .stream()
                .map(this::mapCampaignSummary)
                .toList();
    }

    @Override
    public CampaignDetailResponse getPartnershipCampaignById(UUID campaignId) {
        return mapCampaignDetail(getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId()));
    }

    @Override
    @Transactional
    public CampaignDetailResponse updatePartnershipCampaign(UUID campaignId, CreatePartnershipCampaignRequest request) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được sửa campaign ở trạng thái DRAFT");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());
        campaign.setCampaignName(request.getCampaignName());
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setRegistrationDate(request.getRegistrationDate());
        campaign.setRegistrationDeadline(request.getRegistrationDeadline());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setMaxStudentsPerSchool(request.getMaxStudentsPerSchool());
        campaign.setTotalStudentQuota(request.getTotalStudentQuota());
        campaign.setTopRankingCount(
                request.getTopRankingCount() != null ? request.getTopRankingCount() : campaign.getTopRankingCount());
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaignRepository.save(campaign);

        if (request.getRewards() != null) {
            campaignRewardService.saveRewards(campaign, campaign.getCreatorPartnership(), request.getRewards());
        }

        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public void inviteSchools(UUID campaignId, InviteSchoolsRequest request) {
        Partnership partnership = getCurrentPartnership();
        Campaign campaign = getPartnershipCampaignOwned(campaignId, partnership.getId());
        if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            throw new BadRequestException("Chỉ hỗ trợ mời school cho partnership campaign");
        }

        // Check maxSchoolsPerCampaign quota
        Subscription partnershipSub = getActivePartnershipSubscription(partnership);
        // Only count truly new schools (not already invited)
        List<School> schools = schoolRepository.findAllById(request.getSchoolIds());
        long newSchoolCount = schools.stream()
                .filter(s -> campaignSchoolParticipateRepository.findByCampaignIdAndSchoolId(campaignId, s.getId()).isEmpty())
                .count();
        if (newSchoolCount > 0) {
            checkMaxSchoolsPerCampaign(partnershipSub, campaignId, (int) newSchoolCount);
        }

        for (School school : schools) {
            CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository
                    .findByCampaignIdAndSchoolId(campaignId, school.getId())
                    .orElseGet(CampaignSchoolParticipate::new);
            invitation.setCampaign(campaign);
            invitation.setSchool(school);
            invitation.setStatus(ParticipationStatus.INVITED);
            campaignSchoolParticipateRepository.save(invitation);
        }
    }

    @Override
    @Transactional
    public CampaignDetailResponse activatePartnershipCampaign(UUID campaignId) {
        Partnership partnership = getCurrentPartnership();
        Campaign campaign = getPartnershipCampaignOwned(campaignId, partnership.getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được kích hoạt campaign ở trạng thái DRAFT");
        }
        // Check campaign per month quota
        Subscription partnershipSub = getActivePartnershipSubscription(partnership);
        checkCampaignPerMonthQuota(partnershipSub, null, partnership);

        ensureHasAtLeastOneGameAndQuiz(campaign);
        campaign.setPartnershipStatus(PartnershipCampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse setPartnershipCampaignDraft(UUID campaignId) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.SCHEDULED) {
            throw new BadRequestException("Chỉ được chuyển về DRAFT khi campaign ở trạng thái SCHEDULED");
        }
        campaign.setPartnershipStatus(PartnershipCampaignStatus.DRAFT);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse cancelPartnershipCampaign(UUID campaignId) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        PartnershipCampaignStatus ps = campaign.getPartnershipStatus();
        if (ps != PartnershipCampaignStatus.SCHEDULED
                && ps != PartnershipCampaignStatus.JOINING) {
            throw new BadRequestException("Chỉ được hủy campaign ở trạng thái SCHEDULED, JOINING");
        }
        campaign.setPartnershipStatus(PartnershipCampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        notifyStakeholdersOfCampaignCancellation(campaign);
        invalidateInvitationsOnCampaignCancel(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public void deletePartnershipCampaign(UUID campaignId) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được xóa campaign ở trạng thái DRAFT");
        }
        campaign.setActive(false);
        campaignRepository.save(campaign);
    }

    private record PartnershipInviteExtras(
            List<PartnershipInvitationRoundBriefResponse> rounds,
            List<CampaignRewardResponse> rewards) {
    }

    private PartnershipInviteExtras loadPartnershipInviteExtras(UUID campaignId) {
        List<PartnershipInvitationRoundBriefResponse> rounds = campaignRoundRepository
                .findByCampaignIdOrderByRoundNumberAsc(campaignId)
                .stream()
                .map(r -> PartnershipInvitationRoundBriefResponse.builder()
                        .roundNumber(r.getRoundNumber())
                        .roundName(r.getRoundName())
                        .maxParticipants(r.getMaxParticipants())
                        .advanceCount(r.getAdvanceCount())
                        .isFinalRound(Boolean.TRUE.equals(r.getIsFinalRound()))
                        .build())
                .toList();
        List<CampaignRewardResponse> rewards = campaignRewardService.getRewards(campaignId);
        return new PartnershipInviteExtras(rounds, rewards);
    }

    @Override
    public List<PartnershipInvitationSummaryResponse> getPartnershipInvitationSummariesForSchool() {
        School school = getCurrentSchool();
        return campaignSchoolParticipateRepository
                .findBySchoolIdOrderByCreatedAtDesc(school.getId())
                .stream()
                .filter(i -> i.getCampaign().getCampaignType() == CampaignType.PARTNERSHIP_EVENT)
                .filter(i -> i.getInvitationSentAt() != null)
                .map(i -> {
                    Campaign c = i.getCampaign();
                    return PartnershipInvitationSummaryResponse.builder()
                            .invitationId(i.getId())
                            .campaignId(c.getId())
                            .campaignCode(c.getCampaignCode())
                            .campaignName(c.getCampaignName())
                            .status(i.getStatus())
                            .campaignPartnershipStatus(c.getPartnershipStatus())
                            .invitationSentAt(i.getInvitationSentAt())
                            .participationConfirmedAt(i.getParticipationConfirmedAt())
                            .studentsEnrolled(i.getStudentsEnrolled())
                            .partnershipName(c.getCreatorPartnership() != null
                                    ? c.getCreatorPartnership().getOrganizationName()
                                    : null)
                            .startDate(c.getStartDate())
                            .endDate(c.getEndDate())
                            .registrationDeadline(c.getRegistrationDeadline())
                            .maxStudentsPerSchool(c.getMaxStudentsPerSchool())
                            .build();
                })
                .toList();
    }

    @Override
    public PartnershipInvitationDetailResponse getPartnershipInvitationDetailForSchool(UUID invitationId) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate i = campaignSchoolParticipateRepository
                .findByIdAndSchoolId(invitationId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        Campaign c = i.getCampaign();
        if (c.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            throw new NotFoundException("Không tìm thấy lời mời");
        }
        if (i.getInvitationSentAt() == null) {
            throw new BadRequestException("Lời mời chưa được gửi");
        }

        PartnershipInviteExtras ex = loadPartnershipInviteExtras(c.getId());
        return PartnershipInvitationDetailResponse.builder()
                .invitationId(i.getId())
                .campaignId(c.getId())
                .campaignCode(c.getCampaignCode())
                .campaignName(c.getCampaignName())
                .description(c.getDescription())
                .bannerImageUrl(c.getBannerImageUrl())
                .status(i.getStatus())
                .invitationSentAt(i.getInvitationSentAt())
                .participationConfirmedAt(i.getParticipationConfirmedAt())
                .registrationDate(c.getRegistrationDate())
                .registrationDeadline(c.getRegistrationDeadline())
                .invitationDate(c.getInvitationDate())
                .invitationDeadline(c.getInvitationDeadline())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .maxStudentsPerSchool(c.getMaxStudentsPerSchool())
                .totalStudentQuota(c.getTotalStudentQuota())
                .totalRounds(c.getTotalRounds())
                .studentsEnrolled(i.getStudentsEnrolled())
                .partnershipName(c.getCreatorPartnership() != null
                        ? c.getCreatorPartnership().getOrganizationName()
                        : null)
                .topRankingCount(c.getTopRankingCount())
                .rounds(ex.rounds())
                .rewards(ex.rewards())
                .build();
    }

    @Override
    @Transactional
    public void acceptPartnershipInvitation(UUID invitationId) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository
                .findByIdAndSchoolId(invitationId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        if (invitation.getStatus() != ParticipationStatus.INVITED) {
            throw new BadRequestException("Lời mời đã được xử lý");
        }
        if (invitation.getInvitationSentAt() == null) {
            throw new BadRequestException("Lời mời chưa được gửi");
        }
        invitation.setStatus(ParticipationStatus.APPROVED);
        invitation.setParticipationConfirmedAt(LocalDateTime.now());
        campaignSchoolParticipateRepository.save(invitation);
    }

    @Override
    @Transactional
    public void rejectPartnershipInvitation(UUID invitationId) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository
                .findByIdAndSchoolId(invitationId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        if (invitation.getStatus() != ParticipationStatus.INVITED) {
            throw new BadRequestException("Lời mời đã được xử lý");
        }
        if (invitation.getInvitationSentAt() == null) {
            throw new BadRequestException("Lời mời chưa được gửi");
        }
        invitation.setStatus(ParticipationStatus.REJECTED);
        campaignSchoolParticipateRepository.save(invitation);
    }

    private CampaignSchoolParticipate getSchoolInvitationForAssign(UUID invitationId, UUID schoolId) {
        CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository
                .findByIdAndSchoolId(invitationId, schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        if (invitation.getStatus() != ParticipationStatus.APPROVED) {
            throw new BadRequestException("School phải accept lời mời trước khi phân công học sinh");
        }
        Campaign campaign = invitation.getCampaign();
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.JOINING) {
            throw new BadRequestException("Chỉ được phân công học sinh khi campaign đang JOINING");
        }
        return invitation;
    }

    @Override
    public PartnershipInvitationAssignedStudentsResponse getAssignedStudentsForPartnershipInvitation(
            UUID invitationId) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = getSchoolInvitationForAssign(invitationId, school.getId());
        Campaign campaign = invitation.getCampaign();

        List<CampaignParticipant> current = campaignParticipantRepository
                .findByCampaignIdAndSchoolIdAndIsActiveTrueOrderByCreatedAtAsc(campaign.getId(), school.getId());
        List<PartnershipInvitationAssignedStudentResponse> selected = current.stream()
                .map(p -> PartnershipInvitationAssignedStudentResponse.builder()
                        .studentId(p.getStudent().getId())
                        .studentCode(p.getStudent().getStudentCode())
                        .fullName(p.getStudent().getFullName())
                        .gradeLevel(p.getStudent().getGradeLevel())
                        .className(p.getStudent().getClassName())
                        .parentApprovalStatus(p.getParentApprovalStatus())
                        .invitationSentAt(p.getInvitationSentAt())
                        .build())
                .toList();

        Integer max = campaign.getMaxStudentsPerSchool();
        int selectedCount = selected.size();
        Integer remainingSlots = max == null ? null : Math.max(max - selectedCount, 0);

        return PartnershipInvitationAssignedStudentsResponse.builder()
                .invitationId(invitation.getId())
                .campaignId(campaign.getId())
                .campaignName(campaign.getCampaignName())
                .maxStudentsPerSchool(max)
                .selectedCount(selectedCount)
                .remainingSlots(remainingSlots)
                .selectedStudents(selected)
                .build();
    }

    @Override
    @Transactional
    public void replaceAssignedStudentsForPartnershipInvitation(UUID invitationId, AssignStudentsRequest request) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = getSchoolInvitationForAssign(invitationId, school.getId());
        Campaign campaign = invitation.getCampaign();

        List<UUID> requestedStudentIds = request.getStudentIds().stream().distinct().toList();
        Integer maxStudentsPerSchool = campaign.getMaxStudentsPerSchool();
        if (maxStudentsPerSchool != null && requestedStudentIds.size() > maxStudentsPerSchool) {
            throw new BadRequestException(
                    "Số học sinh chọn vượt quá giới hạn mỗi trường: " + maxStudentsPerSchool);
        }

        List<Student> students = studentRepository.findAllById(requestedStudentIds);
        if (students.size() != requestedStudentIds.size()) {
            throw new NotFoundException("Danh sách học sinh chứa id không tồn tại");
        }
        for (Student student : students) {
            if (!student.getSchool().getId().equals(school.getId())) {
                throw new BadRequestException("Có học sinh không thuộc trường hiện tại");
            }
        }

        List<CampaignParticipant> current = campaignParticipantRepository
                .findByCampaignIdAndSchoolIdAndIsActiveTrueOrderByCreatedAtAsc(campaign.getId(), school.getId());
        Set<UUID> requestedSet = new HashSet<>(requestedStudentIds);
        Set<UUID> currentSet = current.stream().map(p -> p.getStudent().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<CampaignParticipant> toRemove = current.stream()
                .filter(p -> !requestedSet.contains(p.getStudent().getId()))
                .toList();
        if (!toRemove.isEmpty()) {
            campaignParticipantRepository.deleteAll(toRemove);
        }

        for (Student student : students) {
            if (currentSet.contains(student.getId())) {
                continue;
            }
            CampaignParticipant participant = new CampaignParticipant();
            participant.setCampaign(campaign);
            participant.setStudent(student);
            participant.setSchool(school);
            participant.setEnrollmentDate(LocalDateTime.now());
            participant.setParentApprovalStatus(ParticipationStatus.PREPARED);
            campaignParticipantRepository.save(participant);
        }
    }

    @Override
    @Transactional
    public void updateRoundGameConfig(UUID roundId, UpdateRoundGameConfigRequest request) {
        User user = getCurrentUser();
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        Campaign campaign = round.getCampaign();
        ensureRoundEditableByCurrentUser(campaign, user);

        GameType gameType = gameTypeRepository.findById(request.getGameTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy game type"));

        Set<UUID> requestedPresetIds = new HashSet<>(request.getSelectedPresetIds());
        Map<UUID, Integer> presetOrder = new HashMap<>();
        for (int i = 0; i < request.getSelectedPresetIds().size(); i++) {
            presetOrder.putIfAbsent(request.getSelectedPresetIds().get(i), i);
        }
        Map<UUID, List<UUID>> presetSubCategoryRequests = normalizePresetSubCategoryConfigs(request);
        if (!presetSubCategoryRequests.keySet().equals(requestedPresetIds)) {
            throw new BadRequestException("Mỗi preset được chọn phải có cấu hình sub-category tương ứng");
        }
        List<GameLevelPreset> selectedPresets = gameLevelPresetRepository
                .findByIdInAndGameTypeId(request.getSelectedPresetIds(), gameType.getId());
        if (selectedPresets.size() != requestedPresetIds.size()) {
            throw new BadRequestException("selectedPresetIds chứa preset không hợp lệ");
        }
        selectedPresets = selectedPresets.stream()
                .sorted(Comparator.comparingInt(preset -> presetOrder.getOrDefault(preset.getId(), Integer.MAX_VALUE)))
                .toList();

        Set<UUID> requestedSubCategoryIds = presetSubCategoryRequests.values().stream()
                .flatMap(Collection::stream)
                .collect(java.util.stream.Collectors.toSet());
        List<WasteSubCategory> activeSubCategories = wasteSubCategoryRepository
                .findByIdInAndIsDeleteFalse(new ArrayList<>(requestedSubCategoryIds));
        if (activeSubCategories.size() != requestedSubCategoryIds.size()) {
            throw new BadRequestException("Có sub-category không hợp lệ hoặc đã bị xóa mềm");
        }
        Map<UUID, WasteSubCategory> subCategoryById = new HashMap<>();
        for (WasteSubCategory subCategory : activeSubCategories) {
            subCategoryById.put(subCategory.getId(), subCategory);
        }

        Map<String, List<UUID>> normalizedPresetSubCategoryConfig = new LinkedHashMap<>();
        for (GameLevelPreset preset : selectedPresets) {
            Set<WasteCategory> allowedCategories = collectAllowedCategories(preset);
            if (allowedCategories.isEmpty()) {
                throw new BadRequestException("Preset chưa được admin cấu hình wasteCategory: " + preset.getId());
            }

            List<UUID> configuredSubCategoryIds = presetSubCategoryRequests.getOrDefault(preset.getId(), List.of());
            if (configuredSubCategoryIds.isEmpty()) {
                throw new BadRequestException("Preset phải chọn ít nhất 1 sub-category: " + preset.getId());
            }

            for (UUID subCategoryId : configuredSubCategoryIds) {
                WasteSubCategory subCategory = subCategoryById.get(subCategoryId);
                if (subCategory == null || !allowedCategories.contains(subCategory.getCategory())) {
                    throw new BadRequestException(
                            "Sub-category không thuộc wasteCategory admin đã cấu hình cho preset: " + preset.getId());
                }
            }
            normalizedPresetSubCategoryConfig.put(preset.getId().toString(), configuredSubCategoryIds);
        }

        // Xóa config cũ rồi tạo mới để tránh lỗi unique constraint trên
        // (campaign_round_id, display_order)
        roundGameConfigRepository.deleteByCampaignRoundId(roundId);
        roundGameConfigRepository.flush();

        RoundGameConfig config = new RoundGameConfig();
        config.setCampaignRound(round);
        config.setGameType(gameType);
        config.setSelectedPresets(selectedPresets);
        config.setPresetSubCategoryConfig(normalizedPresetSubCategoryConfig);
        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            config.setCoinPerSession(null);
        } else {
            config.setCoinPerSession(request.getCoinPerSession());
        }
        config.setDisplayOrder(1);
        config.setCreatedBy(user);
        roundGameConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void bindQuizzesToRound(UUID roundId, List<BindRoundQuizRequest> requests) {
        User user = getCurrentUser();
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));

        boolean isSchool = user.getRole() == Role.PARTNERSHIP_SCHOOL;
        School school = isSchool ? getCurrentSchool() : null;
        Partnership partnership = isSchool ? null : getCurrentPartnership();

        // Mỗi phần tử request: cùng maxAttempts + isRequired cho toàn bộ quizIds trong
        // phần tử đó
        record ValidatedQuiz(Quiz quiz, Integer maxAttempts, Boolean isRequired) {
        }
        List<ValidatedQuiz> validated = new ArrayList<>();
        Set<UUID> uniqueQuizIds = new HashSet<>();
        for (BindRoundQuizRequest req : requests) {
            if (req.getQuizIds() == null || req.getQuizIds().isEmpty()) {
                continue;
            }
            List<UUID> idsInBlock = req.getQuizIds();

            int maxAttempts = req.getMaxAttempts() != null ? req.getMaxAttempts() : 3;
            boolean required = req.getIsRequired() != null ? req.getIsRequired() : true;

            for (UUID quizId : idsInBlock) {
                if (!uniqueQuizIds.add(quizId)) {
                    throw new BadRequestException("Quiz bị trùng: " + quizId);
                }

                Quiz quiz = isSchool
                        ? quizRepository.findByIdAndSchoolIdAndIsActiveTrue(quizId, school.getId())
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz " + quizId))
                        : quizRepository.findByIdAndPartnershipIdAndIsActiveTrue(quizId, partnership.getId())
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz " + quizId));

                validated.add(new ValidatedQuiz(quiz, maxAttempts, required));
            }
        }

        if (validated.isEmpty()) {
            throw new BadRequestException("Cần cung cấp ít nhất 1 quiz hợp lệ");
        }

        // Xóa toàn bộ quiz cũ và flush ngay để tránh duplicate key khi insert lại
        campaignRoundQuizRepository.deleteByCampaignRoundId(roundId);
        campaignRoundQuizRepository.flush();

        // Insert danh sách mới, displayOrder tự động tăng theo thứ tự truyền vào
        for (int i = 0; i < validated.size(); i++) {
            ValidatedQuiz entry = validated.get(i);
            CampaignRoundQuiz roundQuiz = new CampaignRoundQuiz();
            roundQuiz.setCampaignRound(round);
            roundQuiz.setQuiz(entry.quiz());
            roundQuiz.setMaxAttempts(entry.maxAttempts() != null ? entry.maxAttempts() : 3);
            roundQuiz.setDisplayOrder(i + 1);
            roundQuiz.setRequired(entry.isRequired() != null ? entry.isRequired() : true);
            campaignRoundQuizRepository.save(roundQuiz);
        }
    }

    private boolean campaignMatchStudentStatus(Campaign campaign, CampaignParticipant participant,
            StudentCampaignStatusFilter status) {
        String campaignStatus = statusOf(campaign);
        return switch (status) {
            case INVITED -> participant.getInvitationSentAt() != null
                    && (participant.getParentApprovalStatus() == ParticipationStatus.INVITED
                    || participant.getParentApprovalStatus() == ParticipationStatus.PREPARED);
            case ON_GOING -> "ON_GOING".equals(campaignStatus)
                    && participant.getParentApprovalStatus() == ParticipationStatus.APPROVED;
            case COMPLETED -> "COMPLETED".equals(campaignStatus)
                    && participant.getParentApprovalStatus() == ParticipationStatus.APPROVED;
        };
    }

    @Override
    public List<CampaignSummaryResponse> getStudentCampaigns(StudentCampaignStatusFilter status) {
        Student student = getCurrentStudent();
        List<CampaignParticipant> participants = campaignParticipantRepository
                .findByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(student.getId());

        return participants.stream()
                .filter(p -> {
                    String campaignStatus = statusOf(p.getCampaign());
                    boolean isOngoing = "ON_GOING".equals(campaignStatus)
                            && p.getParentApprovalStatus() == ParticipationStatus.APPROVED;
                    boolean isCompleted = "COMPLETED".equals(campaignStatus);

                    if (!isOngoing && !isCompleted) {
                        return false;
                    }

                    if (status != null) {
                        return switch (status) {
                            case ON_GOING -> isOngoing;
                            case COMPLETED -> isCompleted;
                            case INVITED -> false;
                        };
                    }

                    return true;
                })
                .map(p -> mapCampaignSummary(p.getCampaign(), p.getParentApprovalStatus()))
                .toList();
    }

    @Override
    public CampaignDetailResponse getStudentCampaignDetail(UUID campaignId) {
        Student student = getCurrentStudent();
        CampaignParticipant participant = campaignParticipantRepository.findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new NotFoundException("Bạn không tham gia campaign này"));
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        return mapCampaignDetail(campaign, participant.getId());
    }

    @Override
    public StudentCurrentRoundContentResponse getStudentCurrentRoundContent(UUID campaignId) {
        Student student = getCurrentStudent();
        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia campaign này"));

        Campaign campaign = participant.getCampaign();
        ensureStudentInOnGoingCampaign(participant, campaign);

        CampaignRound currentRound = findCurrentRound(campaign);
        ensurePartnershipRoundAccess(participant, currentRound);

        List<StudentRoundGameConfigResponse> games = roundGameConfigRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(currentRound.getId())
                .stream()
                .map(config -> {
                    Integer coinPerSession = null;
                    if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
                        if (config.getCoinPerSession() != null) {
                            coinPerSession = config.getCoinPerSession();
                        } else if (config.getResolvedDifficulty() != null) {
                            coinPerSession = defaultCoinConfigRepository
                                    .findByGameTypeIdAndDifficulty(config.getGameType().getId(),
                                            config.getResolvedDifficulty())
                                    .map(DefaultCoinConfig::getDefaultCoin)
                                    .orElse(0);
                        }
                    }

                    Map<String, List<UUID>> presetSubCategoryConfig = config.getPresetSubCategoryConfig() == null
                            ? Map.of()
                            : config.getPresetSubCategoryConfig();

                    List<StudentRoundPresetConfigResponse> presets = config.getSelectedPresets() == null
                            ? List.of()
                            : config.getSelectedPresets().stream()
                                    .map(preset -> {
                                        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
                                        LocalDateTime endOfDay = startOfDay.plusDays(1);
                                        final UUID configId = config.getId();
                                        final UUID participantIdFinal = participant.getId();

                                        List<StudentPresetLevelConfigResponse> items = preset.getItems() == null
                                                ? List.of()
                                                : preset.getItems().stream()
                                                        .map(item -> {
                                                            long todayAttempts = gameSessionRepository
                                                                    .countByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndSessionStartBetween(
                                                                            participantIdFinal,
                                                                            configId,
                                                                            preset.getId(),
                                                                            item.getLevelNumber(),
                                                                            startOfDay,
                                                                            endOfDay);
                                                            return StudentPresetLevelConfigResponse.builder()
                                                                    .levelNumber(item.getLevelNumber())
                                                                    .itemCount(item.getItemCount())
                                                                    .timeLimitSeconds(item.getTimeLimitSeconds())
                                                                    .scorePerCorrect(item.getScorePerCorrect())
                                                                    .lives(item.getLives())
                                                                    .wasteCategories(
                                                                            item.getWasteCategories() == null ? Set.of()
                                                                                    : item.getWasteCategories())
                                                                    .configJson(item.getConfigJson() == null ? Map.of()
                                                                            : item.getConfigJson())
                                                                    .coinReceived(campaign
                                                                            .getCampaignType() == CampaignType.SCHOOL_INTERNAL
                                                                                    ? gameSessionRepository
                                                                                            .existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndCoinAwardedGreaterThan(
                                                                                                    participantIdFinal,
                                                                                                    configId,
                                                                                                    preset.getId(),
                                                                                                    item.getLevelNumber(),
                                                                                                    0)
                                                                                    : null)
                                                                    .maxDailyAttempts(MAX_PLAYS_PER_LEVEL_PER_DAY)
                                                                    .todayAttempts(todayAttempts)
                                                                    .isPassed(gameSessionRepository.existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndIsCompletedTrueAndIsPassedTrue(
                                                                            participantIdFinal,
                                                                            configId,
                                                                            preset.getId(),
                                                                            item.getLevelNumber()))
                                                                    .build();
                                                        })
                                                        .toList();

                                        List<UUID> configuredSubCategoryIds = presetSubCategoryConfig.getOrDefault(
                                                preset.getId().toString(),
                                                List.of());

                                        return StudentRoundPresetConfigResponse.builder()
                                                .presetId(preset.getId())
                                                .difficulty(preset.getDifficulty())
                                                .configuredSubCategoryIds(configuredSubCategoryIds)
                                                .items(items)
                                                .build();
                                    })
                                    .toList();

                    return StudentRoundGameConfigResponse.builder()
                            .roundGameConfigId(config.getId())
                            .gameTypeId(config.getGameType().getId())
                            .typeCode(config.getGameType().getTypeCode().name())
                            .gameTypeName(config.getGameType().getName())
                            .resolvedDifficulty(config.getResolvedDifficulty())
                            .coinPerSession(coinPerSession)
                            .presets(presets)
                            .build();
                })
                .toList();

        List<RoundQuizBriefResponse> quizzes = campaignRoundQuizRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(currentRound.getId())
                .stream()
                .map(rq -> {
                    int attemptsUsed = quizAttemptRepository.countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
                            participant.getId(), currentRound.getId(), rq.getQuiz().getId());
                    boolean isPassed = quizAttemptRepository
                            .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                                    participant.getId(), currentRound.getId(), rq.getQuiz().getId())
                            .map(QuizAttempt::isPassed)
                            .orElse(false);

                    return RoundQuizBriefResponse.builder()
                            .quizId(rq.getQuiz().getId())
                            .title(rq.getQuiz().getTitle())
                            .difficulty(rq.getQuiz().getDifficulty())
                            .displayOrder(rq.getDisplayOrder())
                            .attemptsUsed(attemptsUsed)
                            .isPassed(isPassed)
                            .maxAttempts(rq.getMaxAttempts())
                            .isRequired(rq.isRequired())
                            .build();
                })
                .toList();

        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());
        LocalDateTime now = LocalDateTime.now();
        CampaignRound nextRound = rounds.stream()
                .filter(r -> r.getStatus() != RoundStatus.CANCELLED)
                .filter(r -> r.getStartTime() != null && r.getStartTime().isAfter(now))
                .findFirst()
                .orElse(null);

        Long secondsToNextRound = nextRound == null ? null : secondsUntil(nextRound.getStartTime());
        Long secondsToCampaignEnd = secondsToNextRound == null ? secondsUntil(campaign.getEndDate()) : null;

        return StudentCurrentRoundContentResponse.builder()
                .campaignId(campaign.getId())
                .roundId(currentRound.getId())
                .roundNumber(currentRound.getRoundNumber())
                .roundName(currentRound.getRoundName())
                .roundStartTime(currentRound.getStartTime())
                .roundEndTime(currentRound.getEndTime())
                .secondsToNextRound(secondsToNextRound)
                .secondsToCampaignEnd(secondsToCampaignEnd)
                .games(games)
                .quizzes(quizzes)
                .build();
    }

    @Override
    public PlayConfigResponse getPlayConfig(UUID campaignId, UUID roundId) {
        Student student = getCurrentStudent();
        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Student không có quyền vào campaign này"));

        ensureStudentInOnGoingCampaign(participant, participant.getCampaign());

        CampaignRound round = campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));
        ensurePartnershipRoundAccess(participant, round);

        if (round.getStatus() != RoundStatus.ACTIVE || LocalDateTime.now().isBefore(round.getStartTime())
                || LocalDateTime.now().isAfter(round.getEndTime())) {
            throw new BadRequestException("Round hiện không ở trạng thái ACTIVE");
        }

        RoundGameConfig config = roundGameConfigRepository
                .findFirstByCampaignRoundIdOrderByDisplayOrderAsc(round.getId())
                .orElseThrow(() -> new NotFoundException("Round chưa được cấu hình game"));

        Integer coinPerSession = null;
        if (participant.getCampaign().getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            if (config.getCoinPerSession() != null) {
                coinPerSession = config.getCoinPerSession();
            } else if (config.getResolvedDifficulty() != null) {
                coinPerSession = defaultCoinConfigRepository
                        .findByGameTypeIdAndDifficulty(config.getGameType().getId(), config.getResolvedDifficulty())
                        .map(DefaultCoinConfig::getDefaultCoin)
                        .orElse(0);
            }
        }

        return PlayConfigResponse.builder()
                .campaignId(campaignId)
                .roundId(roundId)
                .gameTypeId(config.getGameType().getId())
                .gameTypeName(config.getGameType().getName())
                .resolvedDifficulty(config.getResolvedDifficulty())
                .coinPerSession(coinPerSession)
                .quizId(round.getQuiz() != null ? round.getQuiz().getId() : null)
                .quizIds(round.getSelectedQuizzes() == null ? List.of()
                        : round.getSelectedQuizzes().stream().map(BaseEntity::getId).toList())
                .selectedPresetIds(config.getSelectedPresets() == null ? List.of()
                        : config.getSelectedPresets().stream().map(BaseEntity::getId).toList())
                .presetSubCategoryConfig(
                        config.getPresetSubCategoryConfig() == null ? Map.of() : config.getPresetSubCategoryConfig())
                .build();
    }

    private void ensureStudentInOnGoingCampaign(CampaignParticipant participant, Campaign campaign) {
        if (!participant.isActive()) {
            throw new BadRequestException("Bạn không còn hoạt động trong campaign này");
        }
        if (participant.getParentApprovalStatus() != ParticipationStatus.APPROVED) {
            throw new BadRequestException("Student chưa được parent duyệt tham gia campaign");
        }
        if (!"ON_GOING".equals(statusOf(campaign))) {
            throw new BadRequestException("Campaign chưa ở trạng thái ON_GOING");
        }
    }

    private CampaignRound findCurrentRound(Campaign campaign) {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());

        return rounds.stream()
                .filter(round -> round.getStatus() == RoundStatus.ACTIVE
                        && !now.isBefore(round.getStartTime())
                        && !now.isAfter(round.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Hiện tại không có round nào đang diễn ra"));
    }

    private void ensurePartnershipRoundAccess(CampaignParticipant participant, CampaignRound requestedRound) {
        Campaign campaign = participant.getCampaign();
        if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        CampaignRound activeRound = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId())
                .stream()
                .filter(r -> r.getStatus() == RoundStatus.ACTIVE)
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .filter(r -> !now.isBefore(r.getStartTime()) && !now.isAfter(r.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Hiện tại không có round hợp lệ để tham gia"));

        if (!activeRound.getId().equals(requestedRound.getId())) {
            throw new BadRequestException("Round cũ đã kết thúc, bạn chỉ có thể tham gia round hiện tại");
        }

        Integer roundNumber = requestedRound.getRoundNumber();
        if (roundNumber == null || roundNumber <= 1) {
            return;
        }

        CampaignRound previousRound = campaignRoundRepository
                .findByCampaignIdAndRoundNumber(campaign.getId(), roundNumber - 1)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy round trước để kiểm tra điều kiện"));

        boolean advanced = roundLeaderboardRepository.existsByCampaignRoundIdAndStudentIdAndIsAdvancedTrue(
                previousRound.getId(),
                participant.getStudent().getId())
                || campaignRoundParticipantRepository.existsByCampaignRoundIdAndCampaignParticipantIdAndIsAdvancedTrue(
                        previousRound.getId(),
                        participant.getId());

        if (!advanced) {
            throw new BadRequestException("Bạn không đủ điều kiện tham gia round này");
        }
    }

    private long secondsUntil(LocalDateTime target) {
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), target);
        return Math.max(seconds, 0);
    }

    @Override
    public List<LeaderboardEntryResponse> getCampaignLeaderboard(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));

        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT
                && shouldUseCurrentRoundLeaderboardForRole(getCurrentUser().getRole())) {
            return resolvePartnershipDefaultRound(campaign)
                    .map(round -> mapRoundLeaderboard(roundLeaderboardRepository
                            .findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(
                                    round.getId())))
                    .orElse(List.of());
        }

        if (campaign.getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
            return schoolLeaderboardRepository
                    .findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(campaignId)
                    .stream()
                    .map(e -> LeaderboardEntryResponse.builder()
                            .studentId(e.getStudent().getId())
                            .studentName(e.getStudent().getFullName())
                            .schoolId(e.getSchool().getId())
                            .schoolName(e.getSchool().getSchoolName())
                            .combinedAccuracyPercentage(e.getCombinedAccuracyPercentage())
                            .avgTimeSeconds(e.getAvgTimeSeconds())
                            .rank(e.getOverallRank())
                            .totalCoinsEarned(e.getTotalCoinsEarned())
                            .build())
                    .toList();
        }
        return mapRoundLeaderboard(roundLeaderboardRepository
                .findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(campaignId));
    }

    @Override
    public List<LeaderboardEntryResponse> getCampaignRoundLeaderboard(UUID roundId) {
        campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        return mapRoundLeaderboard(roundLeaderboardRepository
                .findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(roundId));
    }

    private List<LeaderboardEntryResponse> mapRoundLeaderboard(List<RoundLeaderboard> entries) {
        return entries.stream()
                .map(e -> LeaderboardEntryResponse.builder()
                        .studentId(e.getStudent().getId())
                        .studentName(e.getStudent().getFullName())
                        .schoolId(e.getSchool().getId())
                        .schoolName(e.getSchool().getSchoolName())
                        .combinedAccuracyPercentage(e.getCombinedAccuracyPercentage())
                        .avgTimeSeconds(e.getAvgTimeSeconds())
                        .rank(e.getOverallRankInRound())
                        .totalCoinsEarned(e.getTotalCoinsEarned())
                        .build())
                .toList();
    }

    private boolean shouldUseCurrentRoundLeaderboardForRole(Role role) {
        return role == Role.STUDENT
                || role == Role.PARENT
                || role == Role.PARTNERSHIP_SCHOOL
                || role == Role.THIRD_PARTY_PARTNERSHIP;
    }

    private Optional<CampaignRound> resolvePartnershipDefaultRound(Campaign campaign) {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());

        Optional<CampaignRound> current = rounds.stream()
                .filter(r -> r.getStatus() == RoundStatus.ACTIVE)
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .filter(r -> !now.isBefore(r.getStartTime()) && !now.isAfter(r.getEndTime()))
                .findFirst();
        if (current.isPresent()) {
            return current;
        }

        return rounds.stream()
                .filter(r -> r.getStatus() != RoundStatus.CANCELLED)
                .filter(r -> r.getStartTime() != null && !r.getStartTime().isAfter(now))
                .reduce((first, second) -> second);
    }

    @Override
    public List<ParentCampaignInvitationResponse> getParentCampaignInvitations(ParticipationStatus status) {
        Parent parent = getCurrentParent();
        List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
        List<UUID> studentIds = links.stream().map(link -> link.getStudent().getId()).toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }

        List<CampaignParticipant> participants;
        if (status != null) {
            participants = campaignParticipantRepository
                    .findByStudentIdInAndParentApprovalStatusAndIsActiveTrue(studentIds, status);
        } else {
            // INVITED: da gui loi moi chinh thuc; PREPARED + invitationSentAt: du lieu partnership cu (truoc khi scheduler set INVITED)
            List<CampaignParticipant> invited = campaignParticipantRepository
                    .findByStudentIdInAndParentApprovalStatusAndIsActiveTrueAndInvitationSentAtIsNotNull(
                            studentIds, ParticipationStatus.INVITED);
            List<CampaignParticipant> legacyPrepared = campaignParticipantRepository
                    .findByStudentIdInAndParentApprovalStatusAndIsActiveTrueAndInvitationSentAtIsNotNull(
                            studentIds, ParticipationStatus.PREPARED);
            Set<UUID> seen = new HashSet<>();
            participants = new ArrayList<>();
            for (CampaignParticipant p : invited) {
                if (seen.add(p.getId())) {
                    participants.add(p);
                }
            }
            for (CampaignParticipant p : legacyPrepared) {
                if (seen.add(p.getId())) {
                    participants.add(p);
                }
            }
        }

        participants = participants.stream()
                .filter(p -> p.getInvitationSentAt() != null)
                .filter(p -> {
                    String campaignStatus = statusOf(p.getCampaign());
                    return "INVITING".equals(campaignStatus)
                            || "EXTENDED".equals(campaignStatus)
                            || "JOINING".equals(campaignStatus);
                })
                .toList();

        return participants.stream()
                .map(p -> ParentCampaignInvitationResponse.builder()
                        .campaignId(p.getCampaign().getId())
                        .campaignName(p.getCampaign().getCampaignName())
                        .studentId(p.getStudent().getId())
                        .studentName(p.getStudent().getFullName())
                        .parentApprovalStatus(p.getParentApprovalStatus())
                        .rejectionReason(p.getRejectionReason())
                        .invitationDeadline(p.getCampaign().getInvitationDeadline())
                        .build())
                .toList();
    }

    private void parentHandleJoin(UUID campaignId, ParentCampaignApprovalRequest request, ParticipationStatus status) {
        Parent parent = getCurrentParent();
        if (!studentParentLinkRepository.existsByStudentIdAndParentId(request.getStudentId(), parent.getId())) {
            throw new BadRequestException("Phụ huynh không có liên kết hợp lệ với học sinh");
        }

        // Bắt buộc nhập reason khi reject
        if (status == ParticipationStatus.REJECTED
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("Vui lòng nhập lý do từ chối");
        }

        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, request.getStudentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời tham gia campaign"));

        if (participant.getInvitationSentAt() == null) {
            throw new BadRequestException("Lời mời chưa được gửi");
        }

        Campaign campaign = participant.getCampaign();
        String campaignStatus = statusOf(campaign);
        if (!"INVITING".equals(campaignStatus) && !"EXTENDED".equals(campaignStatus)
                && !"JOINING".equals(campaignStatus)) {
            throw new BadRequestException("Chỉ xử lý duyệt khi campaign đang INVITING, EXTENDED");
        }
        ParticipationStatus approval = participant.getParentApprovalStatus();
        boolean awaitingParent = approval == ParticipationStatus.INVITED
                || (approval == ParticipationStatus.PREPARED && participant.getInvitationSentAt() != null);
        if (!awaitingParent) {
            throw new BadRequestException("Lời mời đã được xử lý trước đó");
        }

        participant.setParentApprovalStatus(status);
        participant.setParentApprovedBy(parent);
        participant.setParentApprovedAt(LocalDateTime.now());
        if (status == ParticipationStatus.REJECTED) {
            participant.setRejectionReason(request.getReason());
        }
        campaignParticipantRepository.save(participant);

        // Khi parent APPROVED: tang studentsEnrolled tren CampaignSchoolParticipate cua
        // truong
        if (status == ParticipationStatus.APPROVED) {
            campaignSchoolParticipateRepository
                    .findByCampaignIdAndSchoolId(campaign.getId(), participant.getSchool().getId())
                    .ifPresent(sp -> {
                        sp.setStudentsEnrolled(sp.getStudentsEnrolled() + 1);
                        campaignSchoolParticipateRepository.save(sp);
                    });
        }

        // Thong bao cho hoc sinh sau khi phu huynh xu ly duyet
        User studentUser = participant.getStudent().getUser();
        if (status == ParticipationStatus.APPROVED) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(studentUser.getId())
                    .type(NotificationType.CAMPAIGN_JOINING)
                    .title("Bạn đã tham gia chiến dịch!")
                    .message("Phụ huynh đã duyệt cho bạn tham gia chiến dịch \""
                            + campaign.getCampaignName() + "\". Hãy sẵn sàng thi đấu!")
                    .referenceType("campaign")
                    .referenceId(campaign.getId())
                    .sendEmail(false)
                    .build());
        } else {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(studentUser.getId())
                    .type(NotificationType.CAMPAIGN_JOINING)
                    .title("Lời mời tham gia bị từ chối")
                    .message("Phụ huynh đã từ chối cho bạn tham gia chiến dịch \""
                            + campaign.getCampaignName() + "\".")
                    .referenceType("campaign")
                    .referenceId(campaign.getId())
                    .sendEmail(false)
                    .build());
        }
    }

    @Override
    @Transactional
    public void parentApproveJoin(UUID campaignId, ParentCampaignApprovalRequest request) {
        parentHandleJoin(campaignId, request, ParticipationStatus.APPROVED);
    }

    @Override
    @Transactional
    public void parentRejectJoin(UUID campaignId, ParentCampaignApprovalRequest request) {
        parentHandleJoin(campaignId, request, ParticipationStatus.REJECTED);
    }

    @Override
    public List<CampaignProgressResponse> getParentStudentProgress(UUID studentId) {
        Parent parent = getCurrentParent();
        if (!studentParentLinkRepository.existsByStudentIdAndParentId(studentId, parent.getId())) {
            throw new BadRequestException("Phụ huynh không có quyền xem tiến độ của học sinh này");
        }

        List<CampaignParticipant> participants = campaignParticipantRepository
                .findByStudentIdAndIsActiveTrue(studentId);
        return participants.stream()
                .map(p -> {
                    int completedRounds = campaignRoundParticipantRepository
                            .countByCampaignParticipantIdAndCompletedAtIsNotNull(p.getId());
                    return CampaignProgressResponse.builder()
                            .campaignId(p.getCampaign().getId())
                            .campaignName(p.getCampaign().getCampaignName())
                            .campaignStatus(statusOf(p.getCampaign()))
                            .parentApprovalStatus(p.getParentApprovalStatus())
                            .totalRounds(p.getCampaign().getTotalRounds())
                            .completedRounds(completedRounds)
                            .build();
                })
                .toList();
    }
}
