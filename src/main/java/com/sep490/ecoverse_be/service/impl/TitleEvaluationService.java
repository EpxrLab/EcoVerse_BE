package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.TitleCriteriaType;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.CampaignTitleRepository;
import com.sep490.ecoverse_be.repository.RoundLeaderboardRepository;
import com.sep490.ecoverse_be.repository.StudentTitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TitleEvaluationService {

    private final CampaignTitleRepository campaignTitleRepository;
    private final StudentTitleRepository studentTitleRepository;
    private final RoundLeaderboardRepository roundLeaderboardRepository;
    private final ApplicationEventPublisher eventPublisher;

    // DTO nội bộ: tổng hợp chỉ số của mỗi học sinh trên tất cả các vòng
    private record StudentAggregate(
            Student student,
            BigDecimal bestAccuracy,   // MAX combinedAccuracyPercentage qua các vòng
            BigDecimal bestTime,       // MIN avgTimeSeconds qua các vòng
            BigDecimal avgAccuracy,    // AVG combinedAccuracyPercentage
            BigDecimal avgTime,        // AVG avgTimeSeconds
            int totalGames,
            int totalQuizzes
    ) {}

    /**
     * Đánh giá tất cả danh hiệu của campaign và trao cho học sinh thỏa mãn tiêu chí.
     * Gọi khi campaign chuyển sang trạng thái COMPLETED.
     */
    @Transactional
    public void evaluateTitles(Campaign campaign) {
        List<CampaignTitle> titles = campaignTitleRepository.findByCampaignIdAndIsActiveTrue(campaign.getId());
        if (titles.isEmpty()) {
            log.info("[TitleEvaluation] Campaign '{}' không có danh hiệu nào để đánh giá.", campaign.getCampaignName());
            return;
        }

        List<RoundLeaderboard> allEntries = roundLeaderboardRepository.findByCampaignId(campaign.getId());
        if (allEntries.isEmpty()) {
            log.info("[TitleEvaluation] Campaign '{}' chưa có dữ liệu bảng xếp hạng.", campaign.getCampaignName());
            return;
        }

        // Tổng hợp chỉ số cho từng học sinh qua tất cả các vòng
        Map<UUID, StudentAggregate> aggregates = buildAggregates(allEntries);
        if (aggregates.isEmpty()) return;

        for (CampaignTitle title : titles) {
            if (title.getCriteriaType() == TitleCriteriaType.CUSTOM) {
                log.debug("[TitleEvaluation] Bỏ qua danh hiệu CUSTOM: {}", title.getTitleName());
                continue;
            }
            List<Student> winners = findWinners(title, aggregates);
            for (Student winner : winners) {
                awardTitle(winner, title, campaign);
            }
        }

        log.info("[TitleEvaluation] Hoàn thành đánh giá danh hiệu cho campaign '{}'.", campaign.getCampaignName());
    }

    // ============================================================
    // Tổng hợp chỉ số học sinh qua tất cả các vòng
    // ============================================================

    private Map<UUID, StudentAggregate> buildAggregates(List<RoundLeaderboard> entries) {
        // Nhóm tất cả entries theo studentId
        Map<UUID, List<RoundLeaderboard>> byStudent = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getStudent().getId()));

        Map<UUID, StudentAggregate> result = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<RoundLeaderboard>> entry : byStudent.entrySet()) {
            List<RoundLeaderboard> studentEntries = entry.getValue();
            Student student = studentEntries.get(0).getStudent();

            BigDecimal bestAcc = studentEntries.stream()
                    .map(RoundLeaderboard::getCombinedAccuracyPercentage)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            BigDecimal bestTime = studentEntries.stream()
                    .map(RoundLeaderboard::getAvgTimeSeconds)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            BigDecimal sumAcc = studentEntries.stream()
                    .map(RoundLeaderboard::getCombinedAccuracyPercentage)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long accCount = studentEntries.stream()
                    .filter(e -> e.getCombinedAccuracyPercentage() != null).count();
            BigDecimal avgAcc = accCount > 0 ? sumAcc.divide(BigDecimal.valueOf(accCount), 2, java.math.RoundingMode.HALF_UP) : null;

            BigDecimal sumTime = studentEntries.stream()
                    .map(RoundLeaderboard::getAvgTimeSeconds)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long timeCount = studentEntries.stream()
                    .filter(e -> e.getAvgTimeSeconds() != null).count();
            BigDecimal avgTime = timeCount > 0 ? sumTime.divide(BigDecimal.valueOf(timeCount), 2, java.math.RoundingMode.HALF_UP) : null;

            int totalGames = studentEntries.stream().mapToInt(RoundLeaderboard::getGamesCompleted).sum();
            int totalQuizzes = studentEntries.stream().mapToInt(RoundLeaderboard::getQuizzesCompleted).sum();

            result.put(entry.getKey(), new StudentAggregate(student, bestAcc, bestTime, avgAcc, avgTime, totalGames, totalQuizzes));
        }
        return result;
    }

    // ============================================================
    // Tìm học sinh thắng danh hiệu theo từng tiêu chí
    // ============================================================

    private List<Student> findWinners(CampaignTitle title, Map<UUID, StudentAggregate> aggregates) {
        int max = title.getMaxRecipients() > 0 ? title.getMaxRecipients() : 1;
        List<StudentAggregate> all = new ArrayList<>(aggregates.values());

        return switch (title.getCriteriaType()) {
            case FASTEST_COMPLETION -> all.stream()
                    .filter(a -> a.bestTime() != null)
                    .sorted(Comparator.comparing(StudentAggregate::bestTime))
                    .limit(max)
                    .map(StudentAggregate::student)
                    .toList();

            case HIGHEST_ACCURACY -> all.stream()
                    .filter(a -> a.bestAccuracy() != null)
                    .sorted(Comparator.comparing(StudentAggregate::bestAccuracy).reversed())
                    .limit(max)
                    .map(StudentAggregate::student)
                    .toList();

            case BEST_ACCURACY_AND_TIME -> all.stream()
                    .filter(a -> a.avgAccuracy() != null)
                    .sorted(Comparator
                            .comparing(StudentAggregate::avgAccuracy, Comparator.reverseOrder())
                            .thenComparing(a -> a.avgTime() != null ? a.avgTime() : BigDecimal.valueOf(Long.MAX_VALUE)))
                    .limit(max)
                    .map(StudentAggregate::student)
                    .toList();

            case MOST_GAMES_COMPLETED -> all.stream()
                    .filter(a -> a.totalGames() > 0)
                    .sorted(Comparator.comparingInt(StudentAggregate::totalGames).reversed())
                    .limit(max)
                    .map(StudentAggregate::student)
                    .toList();

            case MOST_QUIZZES_PASSED -> all.stream()
                    .filter(a -> a.totalQuizzes() > 0)
                    .sorted(Comparator.comparingInt(StudentAggregate::totalQuizzes).reversed())
                    .limit(max)
                    .map(StudentAggregate::student)
                    .toList();

            default -> List.of();
        };
    }

    // ============================================================
    // Trao danh hiệu + gửi thông báo
    // ============================================================

    private void awardTitle(Student student, CampaignTitle title, Campaign campaign) {
        // Tránh trao trùng (do constraint unique đã có, nhưng kiểm tra trước để rõ ràng)
        if (studentTitleRepository.existsByStudentIdAndCampaignTitleId(student.getId(), title.getId())) {
            log.debug("[TitleEvaluation] Học sinh {} đã có danh hiệu '{}', bỏ qua.",
                    student.getFullName(), title.getTitleName());
            return;
        }

        String displayText = title.getDisplayFormat()
                .replace("{campaignName}", campaign.getCampaignName())
                .replace("{studentName}", student.getFullName());

        StudentTitle studentTitle = new StudentTitle();
        studentTitle.setStudent(student);
        studentTitle.setCampaignTitle(title);
        studentTitle.setCampaign(campaign);
        studentTitle.setDisplayText(displayText);
        studentTitle.setDisplayed(true);
        StudentTitle saved = studentTitleRepository.save(studentTitle);

        log.info("[TitleEvaluation] Trao danh hiệu '{}' cho học sinh '{}' trong chiến dịch '{}'.",
                title.getTitleName(), student.getFullName(), campaign.getCampaignName());

        // Gửi thông báo cho học sinh
        eventPublisher.publishEvent(NotificationEvent.builder()
                .recipientUserId(student.getUser().getId())
                .type(NotificationType.TITLE_EARNED)
                .title("Bạn đã nhận được danh hiệu!")
                .message("Chúc mừng! Bạn nhận được danh hiệu \"" + title.getTitleName()
                        + "\" tại chiến dịch \"" + campaign.getCampaignName() + "\".")
                .referenceType("student_title")
                .referenceId(saved.getId())
                .metadata(Map.of(
                        "titleName", title.getTitleName(),
                        "campaignName", campaign.getCampaignName(),
                        "criteriaType", title.getCriteriaType().name()
                ))
                .sendEmail(false)
                .build());
    }
}
