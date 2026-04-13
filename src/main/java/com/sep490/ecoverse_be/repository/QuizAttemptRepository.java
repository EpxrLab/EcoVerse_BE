package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    // Lay tat ca attempt cua student cho 1 quiz trong 1 round (tat ca lan lam)
    List<QuizAttempt> findByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
            UUID participantId, UUID roundId, UUID quizId);

    // Dem so lan student da lam 1 quiz trong round
    int countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
            UUID participantId, UUID roundId, UUID quizId);

    // Lay attempt co diem cao nhat (best of N) - dung cho leaderboard
    Optional<QuizAttempt> findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
            UUID participantId, UUID roundId, UUID quizId);

    // Lay tat ca attempt hoan thanh cua student trong 1 round (de tinh leaderboard)
    List<QuizAttempt> findByCampaignParticipantIdAndCampaignRoundIdAndIsCompletedTrue(
            UUID participantId, UUID roundId);

    // Lay attempt theo id va xac thuc quyen (thuoc student nao)
    Optional<QuizAttempt> findByIdAndCampaignParticipantId(UUID attemptId, UUID participantId);

    // Kiem tra attempt dang mo (chua hoan thanh) cua student cho quiz nay trong round
    Optional<QuizAttempt> findByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedFalse(
            UUID participantId, UUID roundId, UUID quizId);

    @Query("""
            SELECT qa
            FROM QuizAttempt qa
            WHERE qa.campaignParticipant.id = :participantId
              AND qa.campaignRound.campaign.id = :campaignId
              AND qa.isCompleted = true
            """)
    List<QuizAttempt> findCompletedByParticipantAndCampaign(@Param("participantId") UUID participantId,
                                                            @Param("campaignId") UUID campaignId);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.campaignParticipant.student.id = :studentId AND qa.isCompleted = true")
    long countCompletedByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT AVG(qa.scorePercentage) FROM QuizAttempt qa WHERE qa.campaignParticipant.student.id = :studentId AND qa.isCompleted = true")
    Double avgScoreByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.campaignParticipant.student.id = :studentId AND qa.isCompleted = true AND qa.isPassed = true")
    long countPassedByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.campaignParticipant.student.id = :studentId AND qa.isCompleted = true AND qa.createdAt BETWEEN :from AND :to")
    long countCompletedByStudentIdAndDateRange(@Param("studentId") UUID studentId, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT AVG(qa.scorePercentage) FROM QuizAttempt qa WHERE qa.campaignParticipant.student.id = :studentId AND qa.isCompleted = true AND qa.createdAt BETWEEN :from AND :to")
    Double avgScoreByStudentIdAndDateRange(@Param("studentId") UUID studentId, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT AVG(qa.scorePercentage) FROM QuizAttempt qa WHERE qa.campaignParticipant.student.id = :studentId AND qa.isCompleted = true")
    Double avgScoreByStudentIdOverall(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.isCompleted = true")
    long countAllCompleted();
}
