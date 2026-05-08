package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.SubmitGameSessionRequest;
import com.sep490.ecoverse_be.dto.response.GameLevelWasteItemResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionResultResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionStartResponse;
import com.sep490.ecoverse_be.dto.response.StudentGameSessionSummaryResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.enums.RoundStatus;
import com.sep490.ecoverse_be.enums.TransactionType;
import com.sep490.ecoverse_be.enums.WasteCategory;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IStudentGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentGameServiceImpl implements IStudentGameService {

    private static final BigDecimal PASS_THRESHOLD = new BigDecimal("75.00");
    private static final int MAX_PLAYS_PER_LEVEL_PER_DAY = 100;

    private final StudentRepository studentRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final CampaignRoundRepository campaignRoundRepository;
    private final CampaignRoundParticipantRepository campaignRoundParticipantRepository;
    private final RoundGameConfigRepository roundGameConfigRepository;
    private final GameSessionRepository gameSessionRepository;
    private final WasteItemRepository wasteItemRepository;
    private final DefaultCoinConfigRepository defaultCoinConfigRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final RoundLeaderboardRepository roundLeaderboardRepository;
    private final SchoolLeaderboardRepository schoolLeaderboardRepository;
    private final CampaignRoundQuizRepository campaignRoundQuizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    private Student getCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        if (principal.getUser().getRole() != Role.STUDENT) {
            throw new BadRequestException("Chỉ học sinh mới có quyền thao tác game");
        }
        return studentRepository.findByUserId(principal.getUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));
    }

    @Override
    @Transactional
    public StudentGameSessionStartResponse startGameSession(UUID campaignId, UUID roundId, UUID roundGameConfigId,
            UUID presetId, Integer levelNumber) {
        int targetLevel = levelNumber == null ? 1 : levelNumber;
        if (targetLevel < 1) {
            throw new BadRequestException("levelNumber phải >= 1");
        }

        Student student = getCurrentStudent();
        CampaignParticipant participant = getValidatedParticipant(campaignId, student.getId());
        getValidatedRound(campaignId, roundId);

        RoundGameConfig config = roundGameConfigRepository.findByIdAndCampaignRoundId(roundGameConfigId, roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình game trong round"));
        ensurePartnershipRoundAccess(participant, config.getCampaignRound());

        GameLevelPreset preset = resolvePreset(config, presetId);
        GameLevelPresetItem levelItem = resolveLevelItem(preset, targetLevel);

        ensureLevelUnlocked(participant.getId(), config.getId(), preset.getId(), targetLevel);
        ensureDailyPlayQuota(participant.getId(), config.getId(), preset.getId(), targetLevel);

        List<GameLevelWasteItemResponse> wasteItems = resolveLevelWasteItems(config, preset, levelItem);
        if (wasteItems.isEmpty()) {
            throw new BadRequestException("Không có waste item khả dụng cho level đã chọn");
        }

        Map<String, Object> presetSnapshot = buildPresetSnapshot(config, preset, levelItem);

        // Auto-close tất cả session đang mở của student (ở mọi game round/config)
        autoCloseOpenSessionsForStudent(student.getId());

        GameSession session = new GameSession();
        session.setCampaignParticipant(participant);
        session.setRoundGameConfig(config);
        session.setGameLevelPreset(preset);
        session.setCurrentLevel(levelItem.getLevelNumber());
        session.setSessionStart(OffsetDateTime.now());
        session.setPresetSnapshot(presetSnapshot);
        session.setCompleted(false);
        session.setPassed(false);
        session.setTotalItems(0);
        session.setCorrectItems(0);
        session.setIncorrectItems(0);
        gameSessionRepository.save(session);

        return StudentGameSessionStartResponse.builder()
                .sessionId(session.getId())
                .campaignId(campaignId)
                .roundId(roundId)
                .roundGameConfigId(config.getId())
                .gameTypeId(config.getGameType().getId())
                .gameTypeName(config.getGameType().getName())
                .resolvedDifficulty(config.getResolvedDifficulty())
                .levelNumber(levelItem.getLevelNumber())
                .itemCount(levelItem.getItemCount())
                .timeLimitSeconds(levelItem.getTimeLimitSeconds())
                .scorePerCorrect(levelItem.getScorePerCorrect())
                .lives(levelItem.getLives())
                .wasteCategories(levelItem.getWasteCategories() == null ? Set.of() : levelItem.getWasteCategories())
                .sessionStart(session.getSessionStart())
                .presetSnapshot(session.getPresetSnapshot())
                .wasteItems(wasteItems)
                .build();
    }

    @Override
    @Transactional
    public StudentGameSessionResultResponse submitGameSession(UUID sessionId, SubmitGameSessionRequest request) {
        Student student = getCurrentStudent();
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên game"));

        if (!session.getCampaignParticipant().getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Bạn không có quyền nộp phiên game này");
        }
        if (session.isCompleted()) {
            throw new BadRequestException("Phiên game này đã được nộp");
        }

        ensurePartnershipRoundAccess(session.getCampaignParticipant(), session.getRoundGameConfig().getCampaignRound());

        validateSubmitRequest(request);

        OffsetDateTime endTime = OffsetDateTime.now();
        int timeTakenSeconds = request.getTimeTakenSeconds() != null
                ? request.getTimeTakenSeconds()
                : (int) ChronoUnit.SECONDS.between(session.getSessionStart(), endTime);

        int totalItems = request.getTotalItems();
        int correctItems = request.getCorrectItems();
        int incorrectItems = request.getIncorrectItems();

        BigDecimal accuracy = totalItems == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf((double) correctItems * 100 / totalItems).setScale(2, RoundingMode.HALF_UP);

        boolean passed = accuracy.compareTo(PASS_THRESHOLD) > 0;

        session.setSessionEnd(endTime);
        session.setTimeTakenSeconds(Math.max(0, timeTakenSeconds));
        session.setTotalItems(totalItems);
        session.setCorrectItems(correctItems);
        session.setIncorrectItems(incorrectItems);
        session.setAccuracyPercentage(accuracy);
        session.setPassed(passed);
        session.setCompleted(true);

        Integer coinAwarded = resolveGameCoinAward(session);
        session.setCoinAwarded(coinAwarded);
        gameSessionRepository.save(session);

        if (coinAwarded != null && coinAwarded > 0) {
            awardGameCoins(session, coinAwarded);
        }

        updateLeaderboardAfterGameSubmit(session.getCampaignParticipant(),
                session.getRoundGameConfig().getCampaignRound());
        return mapResult(session);
    }

    @Override
    public StudentGameSessionResultResponse getGameSessionResult(UUID sessionId) {
        Student student = getCurrentStudent();
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên game"));
        if (!session.getCampaignParticipant().getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Bạn không có quyền xem phiên game này");
        }
        return mapResult(session);
    }

    @Override
    public List<StudentGameSessionSummaryResponse> getGameSessionHistory(UUID campaignId, UUID roundId,
            UUID roundGameConfigId) {
        Student student = getCurrentStudent();
        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia campaign này"));

        campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));

        roundGameConfigRepository.findByIdAndCampaignRoundId(roundGameConfigId, roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình game trong round"));

        return gameSessionRepository.findByParticipantAndRound(participant.getId(), roundId)
                .stream()
                .filter(gs -> gs.getRoundGameConfig().getId().equals(roundGameConfigId))
                .map(gs -> StudentGameSessionSummaryResponse.builder()
                        .sessionId(gs.getId())
                        .presetId(gs.getGameLevelPreset() != null ? gs.getGameLevelPreset().getId() : null)
                        .currentLevel(gs.getCurrentLevel())
                        .totalItems(gs.getTotalItems())
                        .correctItems(gs.getCorrectItems())
                        .incorrectItems(gs.getIncorrectItems())
                        .accuracyPercentage(gs.getAccuracyPercentage())
                        .timeTakenSeconds(gs.getTimeTakenSeconds())
                        .isPassed(gs.isPassed())
                        .coinAwarded(gs.getCoinAwarded())
                        .sessionStart(gs.getSessionStart())
                        .sessionEnd(gs.getSessionEnd())
                        .build())
                .toList();
    }

    private CampaignParticipant getValidatedParticipant(UUID campaignId, UUID studentId) {
        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, studentId)
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia campaign này"));

        if (!participant.isActive()) {
            throw new BadRequestException("Bạn không còn hoạt động trong campaign này");
        }
        if (participant.getParentApprovalStatus() != ParticipationStatus.APPROVED) {
            throw new BadRequestException("Chưa được phụ huynh duyệt tham gia campaign");
        }

        Campaign campaign = participant.getCampaign();
        boolean onGoing = campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT
                ? campaign.getPartnershipStatus().name().equals("ON_GOING")
                : campaign.getSchoolStatus().name().equals("ON_GOING");
        if (!onGoing) {
            throw new BadRequestException("Campaign chưa ở trạng thái ON_GOING");
        }

        return participant;
    }

    private CampaignRound getValidatedRound(UUID campaignId, UUID roundId) {
        CampaignRound round = campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));

        OffsetDateTime now = OffsetDateTime.now();
        if (round.getStatus() != RoundStatus.ACTIVE
                || now.isBefore(round.getStartTime())
                || now.isAfter(round.getEndTime())) {
            throw new BadRequestException("Round hiện không ở trạng thái ACTIVE");
        }

        return round;
    }

    private GameLevelPreset resolvePreset(RoundGameConfig config, UUID presetId) {
        GameLevelPreset preset = null;
        
        // Return exactly the requested preset if provided and it's among the selected ones
        if (presetId != null && config.getSelectedPresets() != null) {
             preset = config.getSelectedPresets().stream()
                     .filter(p -> p.getId().equals(presetId))
                     .findFirst()
                     .orElseThrow(() -> new BadRequestException("Cấu hình game không có preset được yêu cầu"));
        }
        
        // Fallback backward-compatible resolution if presetId not specifically requested
        if (preset == null) {
             preset = config.getResolvedPreset();
             if (preset == null && config.getSelectedPresets() != null && !config.getSelectedPresets().isEmpty()) {
                 preset = config.getSelectedPresets().get(0);
             }
        }
        
        if (preset == null) {
            throw new BadRequestException("Cấu hình game chưa có preset khả dụng");
        }
        if (preset.getItems() == null || preset.getItems().isEmpty()) {
            throw new BadRequestException("Preset chưa có cấu hình level");
        }
        return preset;
    }

    private GameLevelPresetItem resolveLevelItem(GameLevelPreset preset, int levelNumber) {
        return preset.getItems().stream()
                .filter(i -> i.getLevelNumber() == levelNumber)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy level " + levelNumber + " trong preset"));
    }

    private List<GameLevelWasteItemResponse> resolveLevelWasteItems(RoundGameConfig config,
            GameLevelPreset preset,
            GameLevelPresetItem levelItem) {
        List<WasteItem> candidates;

        List<UUID> configuredSubCategoryIds = config.getPresetSubCategoryConfig() == null
                ? List.of()
                : config.getPresetSubCategoryConfig().getOrDefault(preset.getId().toString(), List.of());

        if (!configuredSubCategoryIds.isEmpty()) {
            candidates = wasteItemRepository
                    .findBySubCategoryIdInAndIsDeleteFalseAndIsActiveTrue(configuredSubCategoryIds);
        } else if (levelItem.getWasteCategories() != null && !levelItem.getWasteCategories().isEmpty()) {
            candidates = wasteItemRepository
                    .findByCategoryInAndIsDeleteFalseAndIsActiveTrue(new ArrayList<>(levelItem.getWasteCategories()));
        } else {
            candidates = wasteItemRepository.findByIsDeleteFalse().stream()
                    .filter(WasteItem::isActive)
                    .toList();
        }

        if (levelItem.getWasteCategories() != null && !levelItem.getWasteCategories().isEmpty()) {
            Set<?> allowed = levelItem.getWasteCategories();
            candidates = candidates.stream()
                    .filter(item -> allowed.contains(item.getCategory()))
                    .toList();
        }

        // Group by top-level WasteCategory to ensure items from ALL available categories
        // are distributed evenly (critical for sorting games with multiple bins).
        Map<WasteCategory, List<WasteItem>> itemsByCategory = candidates.stream()
                .collect(Collectors.groupingBy(WasteItem::getCategory));

        List<List<WasteItem>> listOfCategoryItems = new ArrayList<>();
        for (List<WasteItem> list : itemsByCategory.values()) {
            List<WasteItem> mutableList = new ArrayList<>(list);
            Collections.shuffle(mutableList);
            listOfCategoryItems.add(mutableList);
        }

        Collections.shuffle(listOfCategoryItems);

        List<WasteItem> finalItems = new ArrayList<>();
        int limit = Math.max(0, levelItem.getItemCount());
        boolean hasLimit = limit > 0;

        // Round-robin across categories so every category contributes items
        boolean addedInRound = true;
        while (addedInRound && (!hasLimit || finalItems.size() < limit)) {
            addedInRound = false;
            for (List<WasteItem> categoryItems : listOfCategoryItems) {
                if (hasLimit && finalItems.size() >= limit) {
                    break;
                }
                if (!categoryItems.isEmpty()) {
                    finalItems.add(categoryItems.remove(0));
                    addedInRound = true;
                }
            }
        }

        // Shuffle the selected items for unpredictable display order
        Collections.shuffle(finalItems);

        return finalItems.stream()
                .map(item -> GameLevelWasteItemResponse.builder()
                        .wasteItemId(item.getId())
                        .itemName(item.getItemName())
                        .wasteCategory(item.getCategory())
                        .subCategoryId(item.getSubCategory().getId())
                        .subCategoryCode(item.getSubCategory().getSubCategoryCode())
                        .subCategoryDisplayName(item.getSubCategory().getDisplayName())
                        .imageUrl(item.getImageUrl())
                        .imagePresignedUrl(s3PresignedUrlService.generatePresignedUrl(item.getImageUrl()))
                        .model3dUrl(item.getModel3dUrl())
                        .presignedModel3dUrl(s3PresignedUrlService.generatePresignedUrl(item.getModel3dUrl()))
                        .funFact(item.getFunFact())
                        .decompositionTime(item.getDecompositionTime())
                        .recyclingTips(item.getRecyclingTips())
                        .description(item.getDescription())
                        .build())
                .toList();
    }

    private Map<String, Object> buildPresetSnapshot(RoundGameConfig config,
            GameLevelPreset preset,
            GameLevelPresetItem levelItem) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("gameTypeCode", config.getGameType().getTypeCode());
        snapshot.put("difficulty",
                config.getResolvedDifficulty() != null ? config.getResolvedDifficulty().name() : null);
        snapshot.put("presetId", preset.getId());
        snapshot.put("levelNumber", levelItem.getLevelNumber());
        snapshot.put("itemCount", levelItem.getItemCount());
        snapshot.put("timeLimitSeconds", levelItem.getTimeLimitSeconds());
        snapshot.put("scorePerCorrect", levelItem.getScorePerCorrect());
        snapshot.put("lives", levelItem.getLives());
        snapshot.put("wasteCategories",
                levelItem.getWasteCategories() == null ? Set.of() : levelItem.getWasteCategories());
        snapshot.put("configJson", levelItem.getConfigJson() == null ? Map.of() : levelItem.getConfigJson());

        List<UUID> configuredSubCategoryIds = config.getPresetSubCategoryConfig() == null
                ? List.of()
                : config.getPresetSubCategoryConfig().getOrDefault(preset.getId().toString(), List.of());
        snapshot.put("configuredSubCategoryIds", configuredSubCategoryIds);

        return snapshot;
    }

    private void validateSubmitRequest(SubmitGameSessionRequest request) {
        int total = request.getTotalItems();
        int correct = request.getCorrectItems();
        int incorrect = request.getIncorrectItems();

        if (correct + incorrect > total) {
            throw new BadRequestException("correctItems + incorrectItems không được lớn hơn totalItems");
        }
    }

    private void ensureLevelUnlocked(UUID participantId, UUID roundGameConfigId, UUID presetId, int targetLevel) {
        if (targetLevel <= 1) {
            return;
        }

        int previousLevel = targetLevel - 1;
        boolean passedPreviousLevel = gameSessionRepository
                .existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndIsCompletedTrueAndIsPassedTrue(
                        participantId,
                        roundGameConfigId,
                        presetId,
                        previousLevel);
        if (!passedPreviousLevel) {
            throw new BadRequestException(
                    "Bạn cần pass level " + previousLevel + " của cấu hình này trước khi chơi level " + targetLevel);
        }
    }

    private void ensureDailyPlayQuota(UUID participantId, UUID roundGameConfigId, UUID presetId, int levelNumber) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        long todayPlays = gameSessionRepository
                .countByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndSessionStartBetween(
                        participantId,
                        roundGameConfigId,
                        presetId,
                        levelNumber,
                        startOfDay,
                        endOfDay);
        if (todayPlays >= MAX_PLAYS_PER_LEVEL_PER_DAY) {
            throw new BadRequestException("Level này đã chơi đủ " + MAX_PLAYS_PER_LEVEL_PER_DAY
                    + " lần hôm nay. Vui lòng quay lại vào ngày mai");
        }
    }

    private void ensurePartnershipRoundAccess(CampaignParticipant participant, CampaignRound requestedRound) {
        Campaign campaign = participant.getCampaign();
        if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
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

    private Integer resolveGameCoinAward(GameSession session) {
        Campaign campaign = session.getCampaignParticipant().getCampaign();
        if (campaign.getCampaignType() != CampaignType.SCHOOL_INTERNAL || !session.isPassed()) {
            return null;
        }

        boolean alreadyRewarded = gameSessionRepository
                .existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndCoinAwardedGreaterThan(
                        session.getCampaignParticipant().getId(),
                        session.getRoundGameConfig().getId(),
                        session.getGameLevelPreset() != null ? session.getGameLevelPreset().getId() : null,
                        session.getCurrentLevel(),
                        0
                );
        if (alreadyRewarded) {
            return null;
        }

        RoundGameConfig config = session.getRoundGameConfig();
        if (config.getCoinPerSession() != null) {
            return config.getCoinPerSession();
        }
        if (config.getResolvedDifficulty() != null) {
            return defaultCoinConfigRepository
                    .findByGameTypeIdAndDifficulty(config.getGameType().getId(), config.getResolvedDifficulty())
                    .map(DefaultCoinConfig::getDefaultCoin)
                    .orElse(0);
        }
        return 0;
    }

    private void awardGameCoins(GameSession session, int coinAwarded) {
        Student student = session.getCampaignParticipant().getStudent();
        BigDecimal before = student.getTotalCoins() == null ? BigDecimal.ZERO : student.getTotalCoins();
        BigDecimal delta = BigDecimal.valueOf(coinAwarded);
        BigDecimal after = before.add(delta);

        student.setTotalCoins(after);
        studentRepository.save(student);

        CoinTransaction tx = new CoinTransaction();
        tx.setTransactionCode("TX-GAME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setStudent(student);
        tx.setCampaign(session.getCampaignParticipant().getCampaign());
        tx.setTransactionType(TransactionType.EARN_GAME);
        tx.setAmount(delta);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setReferenceType("GAME_SESSION");
        tx.setReferenceId(session.getId());
        tx.setDescription("Thưởng xu từ phiên game " + session.getRoundGameConfig().getGameType().getName());
        tx.setCreatedBy(student.getUser());
        coinTransactionRepository.save(tx);
    }

    /**
     * Kiểm tra student đã hoàn thành TOÀN BỘ nội dung của round chưa:
     * - Mọi level trong mọi preset của mọi RoundGameConfig phải có ít nhất 1 session isPassed=true
     * - Mọi quiz trong round phải có ít nhất 1 attempt isPassed=true
     */
    private boolean isAllContentCompleted(UUID participantId, CampaignRound round) {
        // 1. Kiểm tra game levels
        List<RoundGameConfig> gameConfigs = roundGameConfigRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(round.getId());
        for (RoundGameConfig config : gameConfigs) {
            List<GameLevelPreset> presets = config.getSelectedPresets();
            if (presets == null || presets.isEmpty()) continue;
            for (GameLevelPreset preset : presets) {
                if (preset.getItems() == null || preset.getItems().isEmpty()) continue;
                for (GameLevelPresetItem item : preset.getItems()) {
                    boolean passed = gameSessionRepository
                            .existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndIsCompletedTrueAndIsPassedTrue(
                                    participantId, config.getId(), preset.getId(), item.getLevelNumber());
                    if (!passed) return false;
                }
            }
        }

        // 2. Kiểm tra quizzes
        List<CampaignRoundQuiz> roundQuizzes = campaignRoundQuizRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(round.getId());
        for (CampaignRoundQuiz rq : roundQuizzes) {
            Optional<QuizAttempt> best = quizAttemptRepository
                    .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                            participantId, round.getId(), rq.getQuiz().getId());
            if (best.isEmpty() || !best.get().isPassed()) return false;
        }
        return true;
    }

    /**
     * Tính game accuracy và avg time theo BEST attempt per level (không phải avg tất cả sessions).
     * Trả về [bestGameAccuracy, bestAvgGameTime, gamesCompletedCount, totalGameCoins].
     */
    private record GameMetrics(BigDecimal accuracy, BigDecimal avgTime, int levelCount, int totalCoins) {}

    private GameMetrics calcBestPerLevelGameMetrics(UUID participantId, CampaignRound round) {
        List<RoundGameConfig> gameConfigs = roundGameConfigRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(round.getId());

        List<BigDecimal> bestAccuracies = new ArrayList<>();
        List<BigDecimal> bestTimes = new ArrayList<>();
        int totalCoins = 0;

        for (RoundGameConfig config : gameConfigs) {
            List<GameLevelPreset> presets = config.getSelectedPresets();
            if (presets == null) continue;
            for (GameLevelPreset preset : presets) {
                if (preset.getItems() == null) continue;
                for (GameLevelPresetItem item : preset.getItems()) {
                    List<GameSession> sessions = gameSessionRepository
                            .findBestByParticipantAndConfigAndPresetAndLevel(
                                    participantId, config.getId(), preset.getId(), item.getLevelNumber());
                    if (!sessions.isEmpty()) {
                        GameSession best = sessions.get(0);
                        if (best.getAccuracyPercentage() != null) bestAccuracies.add(best.getAccuracyPercentage());
                        if (best.getTimeTakenSeconds() != null) bestTimes.add(BigDecimal.valueOf(best.getTimeTakenSeconds()));
                    }
                }
            }
        }
        // Tổng coin từ tất cả completed sessions (không giới hạn best)
        totalCoins = gameSessionRepository.findCompletedByParticipantAndRound(participantId, round.getId())
                .stream().map(GameSession::getCoinAwarded).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();

        return new GameMetrics(average(bestAccuracies), average(bestTimes), bestAccuracies.size(), totalCoins);
    }

    private void updateLeaderboardAfterGameSubmit(CampaignParticipant participant, CampaignRound round) {
        // Chỉ cập nhật leaderboard khi student đã hoàn thành toàn bộ nội dung
        if (!isAllContentCompleted(participant.getId(), round)) {
            log.debug("[Leaderboard] Student {} chưa hoàn thành toàn bộ nội dung round {}, bỏ qua cập nhật leaderboard",
                    participant.getStudent().getId(), round.getId());
            return;
        }

        // Tính game metrics theo best attempt per level
        GameMetrics gameMetrics = calcBestPerLevelGameMetrics(participant.getId(), round);

        // Tính quiz metrics (best attempt per quiz — đã có sẵn)
        List<CampaignRoundQuiz> roundQuizzes = campaignRoundQuizRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(round.getId());
        List<BigDecimal> bestScores = new ArrayList<>();
        List<BigDecimal> bestTimes = new ArrayList<>();
        for (CampaignRoundQuiz rq : roundQuizzes) {
            quizAttemptRepository
                    .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                            participant.getId(), round.getId(), rq.getQuiz().getId())
                    .ifPresent(best -> {
                        bestScores.add(best.getScorePercentage());
                        if (best.getTimeTakenSeconds() != null) bestTimes.add(BigDecimal.valueOf(best.getTimeTakenSeconds()));
                    });
        }
        BigDecimal quizAccuracy = average(bestScores);
        BigDecimal avgQuizTime = average(bestTimes);
        int quizzesCompleted = bestScores.size();
        int totalQuizCoins = quizAttemptRepository.findByCampaignParticipantIdAndCampaignRoundIdAndIsCompletedTrue(
                participant.getId(), round.getId())
                .stream().map(QuizAttempt::getCoinsEarned).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();

        RoundLeaderboard roundLb = roundLeaderboardRepository
                .findByCampaignRoundIdAndStudentId(round.getId(), participant.getStudent().getId())
                .orElseGet(() -> {
                    RoundLeaderboard newEntry = new RoundLeaderboard();
                    newEntry.setCampaign(round.getCampaign());
                    newEntry.setCampaignRound(round);
                    newEntry.setStudent(participant.getStudent());
                    newEntry.setSchool(participant.getSchool());
                    return newEntry;
                });

        roundLb.setGameAccuracyPercentage(gameMetrics.accuracy());
        roundLb.setGamesCompleted(gameMetrics.levelCount());
        roundLb.setQuizAccuracyPercentage(quizAccuracy);
        roundLb.setQuizzesCompleted(quizzesCompleted);
        roundLb.setCombinedAccuracyPercentage(combineMetric(gameMetrics.accuracy(), quizAccuracy));
        roundLb.setAvgTimeSeconds(combineMetric(gameMetrics.avgTime(), avgQuizTime));

        if (round.getCampaign().getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
            roundLb.setTotalCoinsEarned(gameMetrics.totalCoins() + totalQuizCoins);
        } else {
            roundLb.setTotalCoinsEarned(null);
        }

        roundLeaderboardRepository.save(roundLb);
        reRankRound(round.getId());

        if (round.getCampaign().getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
            updateSchoolLeaderboard(participant, round.getCampaign());
        }
    }

    private void updateSchoolLeaderboard(CampaignParticipant participant, Campaign campaign) {
        // School campaign chỉ có 1 round — lấy round đó để check isAllContentCompleted
        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());
        if (rounds.isEmpty()) return;
        CampaignRound round = rounds.get(0);

        if (!isAllContentCompleted(participant.getId(), round)) {
            log.debug("[SchoolLeaderboard] Student {} chưa hoàn thành toàn bộ nội dung campaign {}, bỏ qua",
                    participant.getStudent().getId(), campaign.getId());
            return;
        }

        // Game metrics: best per level trên toàn campaign (school = 1 round)
        GameMetrics gameMetrics = calcBestPerLevelGameMetrics(participant.getId(), round);
        int totalGameCoins = gameMetrics.totalCoins();

        // Quiz metrics: best per quiz
        List<QuizAttempt> completedAttempts = quizAttemptRepository
                .findCompletedByParticipantAndCampaign(participant.getId(), campaign.getId());
        Map<UUID, QuizAttempt> bestByQuiz = new HashMap<>();
        for (QuizAttempt attempt : completedAttempts) {
            UUID quizId = attempt.getQuiz().getId();
            QuizAttempt existing = bestByQuiz.get(quizId);
            if (existing == null) { bestByQuiz.put(quizId, attempt); continue; }
            int scoreCompare = attempt.getScorePercentage().compareTo(existing.getScorePercentage());
            if (scoreCompare > 0) { bestByQuiz.put(quizId, attempt); continue; }
            if (scoreCompare == 0) {
                int cur = attempt.getTimeTakenSeconds() == null ? Integer.MAX_VALUE : attempt.getTimeTakenSeconds();
                int ex = existing.getTimeTakenSeconds() == null ? Integer.MAX_VALUE : existing.getTimeTakenSeconds();
                if (cur < ex) bestByQuiz.put(quizId, attempt);
            }
        }
        BigDecimal quizAccuracy = average(bestByQuiz.values().stream()
                .map(QuizAttempt::getScorePercentage).filter(Objects::nonNull).toList());
        BigDecimal avgQuizTime = average(bestByQuiz.values().stream()
                .map(QuizAttempt::getTimeTakenSeconds).filter(Objects::nonNull).map(BigDecimal::valueOf).toList());
        int quizzesCompleted = bestByQuiz.size();
        int totalQuizCoins = completedAttempts.stream()
                .map(QuizAttempt::getCoinsEarned).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();

        SchoolLeaderboard schoolLb = schoolLeaderboardRepository
                .findByCampaignIdAndStudentId(campaign.getId(), participant.getStudent().getId())
                .orElseGet(() -> {
                    SchoolLeaderboard newEntry = new SchoolLeaderboard();
                    newEntry.setCampaign(campaign);
                    newEntry.setStudent(participant.getStudent());
                    newEntry.setSchool(participant.getSchool());
                    return newEntry;
                });

        schoolLb.setGameAccuracyPercentage(gameMetrics.accuracy());
        schoolLb.setGamesCompleted(gameMetrics.levelCount());
        schoolLb.setQuizAccuracyPercentage(quizAccuracy);
        schoolLb.setQuizzesCompleted(quizzesCompleted);
        schoolLb.setCombinedAccuracyPercentage(combineMetric(gameMetrics.accuracy(), quizAccuracy));
        schoolLb.setAvgTimeSeconds(combineMetric(gameMetrics.avgTime(), avgQuizTime));
        schoolLb.setTotalCoinsEarned(totalGameCoins + totalQuizCoins);

        schoolLeaderboardRepository.save(schoolLb);
        reRankSchoolCampaign(campaign.getId());
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal combineMetric(BigDecimal first, BigDecimal second) {
        if (first != null && second != null) {
            return first.add(second).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        if (first != null) {
            return first;
        }
        return second;
    }

    private void reRankRound(UUID roundId) {
        List<RoundLeaderboard> entries = roundLeaderboardRepository.findByCampaignRoundId(roundId);

        entries.sort(Comparator
                .comparing(RoundLeaderboard::getCombinedAccuracyPercentage,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RoundLeaderboard::getAvgTimeSeconds,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setOverallRankInRound(i + 1);
        }

        Map<UUID, List<RoundLeaderboard>> bySchool = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getSchool().getId(),
                        LinkedHashMap::new, Collectors.toList()));
        bySchool.values().forEach(schoolEntries -> {
            for (int i = 0; i < schoolEntries.size(); i++) {
                schoolEntries.get(i).setSchoolRankInRound(i + 1);
            }
        });

        roundLeaderboardRepository.saveAll(entries);
    }

    private void reRankSchoolCampaign(UUID campaignId) {
        List<SchoolLeaderboard> entries = schoolLeaderboardRepository.findByCampaignId(campaignId);

        entries.sort(Comparator
                .comparing(SchoolLeaderboard::getCombinedAccuracyPercentage,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SchoolLeaderboard::getAvgTimeSeconds,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setOverallRank(i + 1);
        }

        Map<UUID, List<SchoolLeaderboard>> bySchool = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getSchool().getId(),
                        LinkedHashMap::new, Collectors.toList()));
        bySchool.values().forEach(schoolEntries -> {
            for (int i = 0; i < schoolEntries.size(); i++) {
                schoolEntries.get(i).setSchoolRank(i + 1);
            }
        });

        schoolLeaderboardRepository.saveAll(entries);
    }

    private StudentGameSessionResultResponse mapResult(GameSession session) {
        String feedback;
        if (!session.isPassed()) {
            feedback = "Hãy chơi lại và cố gắng hơn";
        } else if (session.getCoinAwarded() != null && session.getCoinAwarded() > 0) {
            feedback = "Chúc mừng bạn đã nhận xu cho level này!";
        } else {
            feedback = "Bạn đã hoàn thành level nhưng không nhận thêm xu vì level này đã được nhận thưởng trước đó";
        }

        return StudentGameSessionResultResponse.builder()
                .sessionId(session.getId())
                .campaignId(session.getCampaignParticipant().getCampaign().getId())
                .roundId(session.getRoundGameConfig().getCampaignRound().getId())
                .roundGameConfigId(session.getRoundGameConfig().getId())
                .currentLevel(session.getCurrentLevel())
                .totalItems(session.getTotalItems())
                .correctItems(session.getCorrectItems())
                .incorrectItems(session.getIncorrectItems())
                .accuracyPercentage(session.getAccuracyPercentage())
                .timeTakenSeconds(session.getTimeTakenSeconds())
                .isPassed(session.isPassed())
                .coinAwarded(session.getCoinAwarded())
                .feedbackMessage(feedback)
                .isCompleted(session.isCompleted())
                .sessionStart(session.getSessionStart())
                .sessionEnd(session.getSessionEnd())
                .build();
    }

    /**
     * Auto-close tất cả session đang mở (chưa submit) của student ở mọi game round/config.
     * Session bị đóng sẽ được đánh dấu completed với 0 điểm (không pass, không nhận xu).
     */
    private void autoCloseOpenSessionsForStudent(UUID studentId) {
        List<GameSession> openSessions = gameSessionRepository.findAllOpenSessionsByStudentId(studentId);
        if (openSessions.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (GameSession gs : openSessions) {
            int timeTaken = gs.getSessionStart() != null
                    ? (int) ChronoUnit.SECONDS.between(gs.getSessionStart(), now)
                    : 0;

            gs.setSessionEnd(now);
            gs.setTimeTakenSeconds(Math.max(0, timeTaken));
            gs.setTotalItems(gs.getTotalItems() > 0 ? gs.getTotalItems() : 0);
            gs.setCorrectItems(0);
            gs.setIncorrectItems(gs.getTotalItems());
            gs.setAccuracyPercentage(BigDecimal.ZERO);
            gs.setPassed(false);
            gs.setCompleted(true);
            gs.setCoinAwarded(null);

            log.info("Auto-closed abandoned game session {} for student {}", gs.getId(), studentId);
        }
        gameSessionRepository.saveAll(openSessions);
    }

    @Override
    public List<StudentGameSessionSummaryResponse> getOpenSessionsByStudentId(UUID studentId) {
        return gameSessionRepository.findAllOpenSessionsByStudentId(studentId)
                .stream()
                .map(gs -> StudentGameSessionSummaryResponse.builder()
                        .sessionId(gs.getId())
                        .currentLevel(gs.getCurrentLevel())
                        .totalItems(gs.getTotalItems())
                        .correctItems(gs.getCorrectItems())
                        .incorrectItems(gs.getIncorrectItems())
                        .accuracyPercentage(gs.getAccuracyPercentage())
                        .timeTakenSeconds(gs.getTimeTakenSeconds())
                        .isPassed(gs.isPassed())
                        .coinAwarded(gs.getCoinAwarded())
                        .sessionStart(gs.getSessionStart())
                        .sessionEnd(gs.getSessionEnd())
                        .build())
                .toList();
    }
}
