package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.entity.CampaignParticipant;
import com.sep490.ecoverse_be.entity.CampaignReward;
import com.sep490.ecoverse_be.entity.CampaignRewardDelivery;
import com.sep490.ecoverse_be.entity.CampaignRound;
import com.sep490.ecoverse_be.entity.CampaignRoundParticipant;
import com.sep490.ecoverse_be.entity.RoundLeaderboard;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import com.sep490.ecoverse_be.enums.RoundStatus;
import com.sep490.ecoverse_be.repository.CampaignParticipantRepository;
import com.sep490.ecoverse_be.repository.CampaignRewardDeliveryRepository;
import com.sep490.ecoverse_be.repository.CampaignRewardRepository;
import com.sep490.ecoverse_be.repository.CampaignRoundRepository;
import com.sep490.ecoverse_be.repository.CampaignRoundParticipantRepository;
import com.sep490.ecoverse_be.repository.RoundLeaderboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignRoundScheduler {

    private final CampaignRoundRepository campaignRoundRepository;
    private final RoundLeaderboardRepository roundLeaderboardRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final CampaignRoundParticipantRepository campaignRoundParticipantRepository;
    private final CampaignRewardRepository campaignRewardRepository;
    private final CampaignRewardDeliveryRepository campaignRewardDeliveryRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoTransitionRoundStatuses() {
        LocalDateTime now = LocalDateTime.now();
        transitionToActive(now);
        transitionToCompleted(now);
    }

    private void transitionToActive(LocalDateTime now) {
        List<CampaignRound> rounds = campaignRoundRepository.findRoundsReadyToActivate(RoundStatus.UPCOMING, now);
        if (rounds.isEmpty()) {
            return;
        }

        for (CampaignRound round : rounds) {
            round.setStatus(RoundStatus.ACTIVE);
            campaignRoundRepository.save(round);
            log.info("[CampaignRoundScheduler] Round '{}' (#{}): UPCOMING -> ACTIVE", round.getRoundName(), round.getRoundNumber());
        }
        log.info("[CampaignRoundScheduler] {} round(s) transitioned to ACTIVE", rounds.size());
    }

    private void transitionToCompleted(LocalDateTime now) {
        List<CampaignRound> rounds = campaignRoundRepository.findRoundsReadyToComplete(RoundStatus.ACTIVE, now);
        if (rounds.isEmpty()) {
            return;
        }

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

        List<RoundLeaderboard> ranked = roundLeaderboardRepository
                .findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(round.getId());
        if (ranked.isEmpty()) {
            return;
        }

        if (Boolean.TRUE.equals(round.getIsFinalRound())) {
            finalizePartnershipWinners(round, ranked);
        } else {
            advanceToNextRound(round, ranked);
        }
    }

    private void advanceToNextRound(CampaignRound round, List<RoundLeaderboard> ranked) {
        int advanceCount = round.getAdvanceCount() == null ? 0 : round.getAdvanceCount();
        if (advanceCount <= 0) {
            return;
        }

        List<RoundLeaderboard> topStudents = ranked.stream().limit(advanceCount).toList();
        Map<String, RoundLeaderboard> advancedKey = topStudents.stream()
                .collect(Collectors.toMap(e -> e.getCampaignRound().getId() + ":" + e.getStudent().getId(), e -> e, (a, b) -> a));

        for (RoundLeaderboard entry : ranked) {
            boolean advanced = advancedKey.containsKey(entry.getCampaignRound().getId() + ":" + entry.getStudent().getId());
            entry.setAdvanced(advanced);
        }
        roundLeaderboardRepository.saveAll(ranked);

        Map<String, CampaignRoundParticipant> existingByParticipant = campaignRoundParticipantRepository
                .findByCampaignRoundId(round.getId())
                .stream()
                .collect(Collectors.toMap(e -> e.getCampaignParticipant().getId().toString(), e -> e, (a, b) -> a));

        for (RoundLeaderboard entry : ranked) {
            Optional<CampaignParticipant> campaignParticipantOpt = campaignParticipantRepository
                    .findByCampaignIdAndStudentIdAndIsActiveTrue(round.getCampaign().getId(), entry.getStudent().getId());
            if (campaignParticipantOpt.isEmpty()) {
                continue;
            }
            CampaignParticipant cp = campaignParticipantOpt.get();

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
            crp.setCompletedAt(LocalDateTime.now());
            campaignRoundParticipantRepository.save(crp);
        }

        Integer roundNumber = round.getRoundNumber();
        if (roundNumber == null) {
            return;
        }
        Optional<CampaignRound> nextRoundOpt = campaignRoundRepository
                .findByCampaignIdAndRoundNumber(round.getCampaign().getId(), roundNumber + 1);
        if (nextRoundOpt.isEmpty()) {
            return;
        }

        CampaignRound nextRound = nextRoundOpt.get();
        for (RoundLeaderboard entry : topStudents) {
            Optional<CampaignParticipant> campaignParticipantOpt = campaignParticipantRepository
                    .findByCampaignIdAndStudentIdAndIsActiveTrue(round.getCampaign().getId(), entry.getStudent().getId());
            if (campaignParticipantOpt.isEmpty()) {
                continue;
            }
            CampaignParticipant cp = campaignParticipantOpt.get();

            campaignRoundParticipantRepository
                    .findByCampaignRoundIdAndCampaignParticipantId(nextRound.getId(), cp.getId())
                    .orElseGet(() -> {
                        CampaignRoundParticipant nextRoundParticipant = new CampaignRoundParticipant();
                        nextRoundParticipant.setCampaignRound(nextRound);
                        nextRoundParticipant.setCampaignParticipant(cp);
                        return campaignRoundParticipantRepository.save(nextRoundParticipant);
                    });
        }

        log.info("[CampaignRoundScheduler] Round '{}' advanced {} student(s) to next round", round.getRoundName(), topStudents.size());
    }

    private void finalizePartnershipWinners(CampaignRound round, List<RoundLeaderboard> ranked) {
        Campaign campaign = round.getCampaign();
        int topRankingCount = campaign.getTopRankingCount() == null ? 0 : campaign.getTopRankingCount();
        if (topRankingCount <= 0) {
            return;
        }

        List<RoundLeaderboard> winners = ranked.stream().limit(topRankingCount).toList();
        Map<Integer, CampaignReward> rewardByRank = campaignRewardRepository
                .findByCampaignIdOrderByRankPositionAsc(campaign.getId())
                .stream()
                .collect(Collectors.toMap(CampaignReward::getRankPosition, r -> r, (a, b) -> a, HashMap::new));

        for (int i = 0; i < winners.size(); i++) {
            RoundLeaderboard winner = winners.get(i);
            Integer rank = winner.getOverallRankInRound() != null ? winner.getOverallRankInRound() : (i + 1);
            CampaignReward reward = rewardByRank.get(rank);
            if (reward == null) {
                continue;
            }

            if (campaignRewardDeliveryRepository.existsByCampaignRewardIdAndStudentId(reward.getId(), winner.getStudent().getId())) {
                continue;
            }

            CampaignRewardDelivery delivery = new CampaignRewardDelivery();
            delivery.setCampaignReward(reward);
            delivery.setCampaign(campaign);
            delivery.setCampaignRound(round);
            delivery.setRoundLeaderboard(winner);
            delivery.setStudent(winner.getStudent());
            delivery.setSchool(winner.getSchool());
            delivery.setLeaderboardRank(rank);
            delivery.setStatus(PartnershipRewardStatus.PREPARING);
            delivery.setPreparingAt(LocalDateTime.now());
            campaignRewardDeliveryRepository.save(delivery);
        }

        log.info("[CampaignRoundScheduler] Final round '{}' selected {} winner(s) for reward delivery", round.getRoundName(), winners.size());
    }
}
