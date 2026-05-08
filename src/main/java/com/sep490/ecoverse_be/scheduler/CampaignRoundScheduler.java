package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.RoundStatus;
import com.sep490.ecoverse_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignRoundScheduler {

    private final CampaignRoundRepository campaignRoundRepository;
    private final RoundLeaderboardRepository roundLeaderboardRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final CampaignRoundParticipantRepository campaignRoundParticipantRepository;
    private final RoundGameConfigRepository roundGameConfigRepository;
    private final CampaignRoundQuizRepository campaignRoundQuizRepository;
    private final GameSessionRepository gameSessionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoTransitionRoundStatuses() {
        OffsetDateTime now = OffsetDateTime.now();
        transitionToActive(now);
        transitionToCompleted(now);
    }

    private void transitionToActive(OffsetDateTime now) {
        List<CampaignRound> rounds = campaignRoundRepository.findRoundsReadyToActivate(RoundStatus.UPCOMING, now);
        if (rounds.isEmpty()) return;
        for (CampaignRound round : rounds) {
            round.setStatus(RoundStatus.ACTIVE);
            campaignRoundRepository.save(round);
            log.info("[CampaignRoundScheduler] Round '{}' (#{}): UPCOMING -> ACTIVE", round.getRoundName(), round.getRoundNumber());
        }
        log.info("[CampaignRoundScheduler] {} round(s) transitioned to ACTIVE", rounds.size());
    }

    private void transitionToCompleted(OffsetDateTime now) {
        List<CampaignRound> rounds = campaignRoundRepository.findRoundsReadyToComplete(RoundStatus.ACTIVE, now);
        if (rounds.isEmpty()) return;
        for (CampaignRound round : rounds) {
            handleRoundCompletion(round);
            round.setStatus(RoundStatus.COMPLETED);
            campaignRoundRepository.save(round);
            log.info("[CampaignRoundScheduler] Round '{}' (#{}): ACTIVE -> COMPLETED", round.getRoundName(), round.getRoundNumber());
        }
        log.info("[CampaignRoundScheduler] {} round(s) transitioned to COMPLETED", rounds.size());
    }

    private void handleRoundCompletion(CampaignRound round) {
        Campaign campaign = round.getCampaign();
        if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            return;
        }

        // Lay tat ca participant APPROVED cua campaign
        List<CampaignParticipant> allParticipants = campaignParticipantRepository
                .findByCampaignIdAndParentApprovalStatusAndIsActiveTrue(campaign.getId(), ParticipationStatus.APPROVED);

        // Voi moi participant chua hoan thanh: tao/update entry score=0, isAdvanced=false
        for (CampaignParticipant participant : allParticipants) {
            if (!isAllContentCompleted(participant.getId(), round)) {
                RoundLeaderboard lb = roundLeaderboardRepository
                        .findByCampaignRoundIdAndStudentId(round.getId(), participant.getStudent().getId())
                        .orElseGet(() -> {
                            RoundLeaderboard newLb = new RoundLeaderboard();
                            newLb.setCampaign(campaign);
                            newLb.setCampaignRound(round);
                            newLb.setStudent(participant.getStudent());
                            newLb.setSchool(participant.getSchool());
                            return newLb;
                        });
                lb.setGameAccuracyPercentage(BigDecimal.ZERO);
                lb.setQuizAccuracyPercentage(BigDecimal.ZERO);
                lb.setCombinedAccuracyPercentage(BigDecimal.ZERO);
                lb.setAvgTimeSeconds(BigDecimal.ZERO);
                lb.setGamesCompleted(0);
                lb.setQuizzesCompleted(0);
                lb.setAdvanced(false);
                roundLeaderboardRepository.save(lb);
                log.info("[CampaignRoundScheduler] Student {} chua hoan thanh round '{}' -> score=0, isAdvanced=false",
                        participant.getStudent().getId(), round.getRoundName());
            }
        }

        // Re-rank sau khi cap nhat cac entry score=0
        reRankRound(round.getId());

        List<RoundLeaderboard> ranked = roundLeaderboardRepository
                .findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(round.getId());
        if (ranked.isEmpty()) return;

        // Final round: delivery records se duoc tao khi campaign chuyen sang COMPLETED (trong CampaignScheduler)
        if (!Boolean.TRUE.equals(round.getIsFinalRound())) {
            advanceToNextRound(round, ranked);
        }
    }

    /**
     * Kiem tra student da hoan thanh TOAN BO noi dung round:
     * - Moi level trong moi preset phai co session isPassed=true
     * - Moi quiz phai co attempt isPassed=true
     */
    private boolean isAllContentCompleted(UUID participantId, CampaignRound round) {
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
     * Re-rank toan bo entry trong round theo combinedAccuracy DESC, avgTime ASC.
     */
    private void reRankRound(UUID roundId) {
        List<RoundLeaderboard> entries = roundLeaderboardRepository.findByCampaignRoundId(roundId);
        entries.sort(Comparator
                .comparing(RoundLeaderboard::getCombinedAccuracyPercentage,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RoundLeaderboard::getAvgTimeSeconds,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        for (int i = 0; i < entries.size(); i++) entries.get(i).setOverallRankInRound(i + 1);
        roundLeaderboardRepository.saveAll(entries);
    }

    private void advanceToNextRound(CampaignRound round, List<RoundLeaderboard> ranked) {
        int advanceCount = round.getAdvanceCount() == null ? 0 : round.getAdvanceCount();
        if (advanceCount <= 0) return;

        // Chi advance nhung student co isAdvanced != false (tuc la da hoan thanh - khong bi set =false o tren)
        List<RoundLeaderboard> eligibleRanked = ranked.stream()
                .filter(e -> !Boolean.FALSE.equals(e.isAdvanced()))
                .toList();

        List<RoundLeaderboard> topStudents = eligibleRanked.stream().limit(advanceCount).toList();
        Set<String> topKeys = topStudents.stream()
                .map(e -> e.getCampaignRound().getId() + ":" + e.getStudent().getId())
                .collect(Collectors.toSet());

        for (RoundLeaderboard entry : ranked) {
            String key = entry.getCampaignRound().getId() + ":" + entry.getStudent().getId();
            // Giu isAdvanced=false cho nhung student chua hoan thanh, chi dat true cho top
            if (!Boolean.FALSE.equals(entry.isAdvanced())) {
                entry.setAdvanced(topKeys.contains(key));
            }
        }
        roundLeaderboardRepository.saveAll(ranked);

        Map<String, CampaignRoundParticipant> existingByParticipant = campaignRoundParticipantRepository
                .findByCampaignRoundId(round.getId())
                .stream()
                .collect(Collectors.toMap(e -> e.getCampaignParticipant().getId().toString(), e -> e, (a, b) -> a));

        for (RoundLeaderboard entry : ranked) {
            Optional<CampaignParticipant> cpOpt = campaignParticipantRepository
                    .findByCampaignIdAndStudentIdAndIsActiveTrue(round.getCampaign().getId(), entry.getStudent().getId());
            if (cpOpt.isEmpty()) continue;
            CampaignParticipant cp = cpOpt.get();

            CampaignRoundParticipant crp = existingByParticipant.get(cp.getId().toString());
            if (crp == null) {
                crp = new CampaignRoundParticipant();
                crp.setCampaignRound(round);
                crp.setCampaignParticipant(cp);
            }
            crp.setAccuracyPercentage(entry.getCombinedAccuracyPercentage());
            crp.setTotalTimeSeconds(entry.getAvgTimeSeconds() == null ? 0 : entry.getAvgTimeSeconds().intValue());
            crp.setRankInRound(entry.getOverallRankInRound());
            crp.setIsAdvanced(entry.isAdvanced());
            crp.setCompletedAt(OffsetDateTime.now());
            campaignRoundParticipantRepository.save(crp);
        }

        Integer roundNumber = round.getRoundNumber();
        if (roundNumber == null) return;
        Optional<CampaignRound> nextRoundOpt = campaignRoundRepository
                .findByCampaignIdAndRoundNumber(round.getCampaign().getId(), roundNumber + 1);
        if (nextRoundOpt.isEmpty()) return;

        CampaignRound nextRound = nextRoundOpt.get();
        for (RoundLeaderboard entry : topStudents) {
            Optional<CampaignParticipant> cpOpt = campaignParticipantRepository
                    .findByCampaignIdAndStudentIdAndIsActiveTrue(round.getCampaign().getId(), entry.getStudent().getId());
            if (cpOpt.isEmpty()) continue;
            CampaignParticipant cp = cpOpt.get();
            campaignRoundParticipantRepository
                    .findByCampaignRoundIdAndCampaignParticipantId(nextRound.getId(), cp.getId())
                    .orElseGet(() -> {
                        CampaignRoundParticipant next = new CampaignRoundParticipant();
                        next.setCampaignRound(nextRound);
                        next.setCampaignParticipant(cp);
                        return campaignRoundParticipantRepository.save(next);
                    });
        }

        log.info("[CampaignRoundScheduler] Round '{}' advanced {} student(s) to next round", round.getRoundName(), topStudents.size());
    }
}
