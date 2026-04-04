package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.QuizAttemptAnswerSubmit;
import com.sep490.ecoverse_be.dto.request.SubmitQuizAttemptRequest;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.RoundStatus;
import com.sep490.ecoverse_be.enums.TransactionType;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IStudentQuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentQuizServiceImpl implements IStudentQuizService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CampaignParticipantRepository campaignParticipantRepository;
    @Autowired
    private CampaignRoundRepository campaignRoundRepository;
        @Autowired
        private CampaignRoundParticipantRepository campaignRoundParticipantRepository;
    @Autowired
    private CampaignRoundQuizRepository campaignRoundQuizRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizQuestionRepository quizQuestionRepository;
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;
    @Autowired
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;
        @Autowired
        private CoinTransactionRepository coinTransactionRepository;
        @Autowired
        private GameSessionRepository gameSessionRepository;
    @Autowired
    private RoundLeaderboardRepository roundLeaderboardRepository;
    @Autowired
    private SchoolLeaderboardRepository schoolLeaderboardRepository;
    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    // Lay thong tin student dang dang nhap
    private Student getCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        return studentRepository.findByUserId(principal.getUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));
    }

    @Override
    @Transactional
    public StudentQuizDetailResponse startQuizAttempt(UUID campaignId, UUID roundId, UUID quizId) {
        Student student = getCurrentStudent();

        // Xac thuc student la participant da duoc parent duyet
        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia campaign này"));
        if (participant.getParentApprovalStatus() != ParticipationStatus.APPROVED) {
            throw new BadRequestException("Chưa được phụ huynh duyệt tham gia campaign");
        }

        // Kiem tra round hop le
        CampaignRound round = campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));
        ensurePartnershipRoundAccess(participant, round);
        if (round.getStatus() != RoundStatus.ACTIVE) {
            throw new BadRequestException("Round chưa bắt đầu hoặc đã kết thúc");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(round.getStartTime()) || now.isAfter(round.getEndTime())) {
            throw new BadRequestException("Không trong thời gian làm bài của round");
        }

        // Kiem tra quiz thuoc round nay
        CampaignRoundQuiz roundQuiz = campaignRoundQuizRepository
                .findByCampaignRoundIdAndQuizId(roundId, quizId)
                .orElseThrow(() -> new NotFoundException("Quiz không thuộc round này"));

        // Kiem tra so lan da lam
        int usedAttempts = quizAttemptRepository
                .countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(participant.getId(), roundId, quizId);
        if (usedAttempts >= roundQuiz.getMaxAttempts()) {
            throw new BadRequestException("Bạn đã dùng hết " + roundQuiz.getMaxAttempts() + " lần làm quiz này");
        }

        // Kiem tra khong co attempt dang mo (chua nop)
        quizAttemptRepository.findByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedFalse(
                participant.getId(), roundId, quizId)
                .ifPresent(a -> {
                    throw new BadRequestException("Bạn đang có bài làm chưa nộp. Hãy nộp bài trước khi bắt đầu lại");
                });

        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz"));

        // Lay danh sach cau hoi
        List<QuizQuestion> questions = quizQuestionRepository
                .findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(quizId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Quiz chưa có câu hỏi");
        }

        // Lay danh sach dap an cho tat ca cau hoi
        List<QuizAnswer> allAnswers = quizAnswerRepository.findByQuestionIn(questions);
        Map<UUID, List<QuizAnswer>> answersByQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        // Tao attempt moi
        QuizAttempt attempt = new QuizAttempt();
        attempt.setCampaignParticipant(participant);
        attempt.setCampaignRound(round);
        attempt.setQuiz(quiz);
        attempt.setAttemptNumber(usedAttempts + 1);
        attempt.setStartTime(now);
        attempt.setTotalQuestions(questions.size());
        attempt.setCompleted(false);
        attempt.setPassed(false);
        quizAttemptRepository.save(attempt);

        // Map cau hoi -> response (xao tron thu tu dap an, khong lo isCorrect)
        List<StudentQuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    List<QuizAnswer> answers = answersByQuestion.getOrDefault(q.getId(), List.of());
                    // Xao tron thu tu dap an de tranh gian lan
                    List<QuizAnswer> shuffled = new ArrayList<>(answers);
                    Collections.shuffle(shuffled);
                    List<StudentAnswerOptionResponse> answerOptions = shuffled.stream()
                            .map(a -> StudentAnswerOptionResponse.builder()
                                    .answerId(a.getId())
                                    .answerText(a.getAnswerText())
                                    .build())
                            .toList();
                    return StudentQuestionResponse.builder()
                            .questionId(q.getId())
                            .questionOrder(q.getQuestionOrder())
                            .questionText(q.getQuestionText())
                            .questionImageUrl(q.getQuestionImageUrl())
                            .questionImagePresignedUrl(s3PresignedUrlService.generatePresignedUrl(q.getQuestionImageUrl()))
                            .answers(answerOptions)
                            .build();
                })
                .toList();

        return StudentQuizDetailResponse.builder()
                .quizId(quizId)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timePerQuestion(quiz.getTimePerQuestion())
                .passScorePercentage(quiz.getPassScorePercentage())
                .questionCount(questions.size())
                .attemptId(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .maxAttempts(roundQuiz.getMaxAttempts())
                .startTime(attempt.getStartTime())
                .questions(questionResponses)
                .build();
    }

    @Override
    @Transactional
    public QuizAttemptResultResponse submitQuizAttempt(UUID attemptId, SubmitQuizAttemptRequest request) {
        Student student = getCurrentStudent();

        // Tim participant cua student
        // Tim attempt thuoc student nay (qua campaignParticipant)
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài làm"));

        // Xac thuc attempt thuoc student hien tai
        if (!attempt.getCampaignParticipant().getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Bạn không có quyền nộp bài này");
        }

        if (attempt.isCompleted()) {
            throw new BadRequestException("Bài làm này đã được nộp rồi");
        }

        Quiz quiz = attempt.getQuiz();
        CampaignParticipant participant = attempt.getCampaignParticipant();
        CampaignRound round = attempt.getCampaignRound();
        ensurePartnershipRoundAccess(participant, round);

        // Lay danh sach cau hoi cua quiz
        List<QuizQuestion> questions = quizQuestionRepository
                .findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(quiz.getId());

        // Lay tat ca dap an cho cac cau hoi
        List<QuizAnswer> allAnswers = quizAnswerRepository.findByQuestionIn(questions);
        Map<UUID, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(BaseEntity::getId, q -> q));
        // Map questionId -> dap an dung
        Map<UUID, QuizAnswer> correctAnswerByQuestion = allAnswers.stream()
                .filter(QuizAnswer::isCorrect)
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));
        // Map answerId -> QuizAnswer
        Map<UUID, QuizAnswer> answerMap = allAnswers.stream()
                .collect(Collectors.toMap(QuizAnswer::getId, a -> a));

        // Cham diem tung cau
        int correctCount = 0;
        List<QuizAttemptAnswer> attemptAnswers = new ArrayList<>();
        List<AnswerResultResponse> answerResults = new ArrayList<>();

        for (QuizAttemptAnswerSubmit submitted : request.getAnswers()) {
            QuizQuestion question = questionMap.get(submitted.getQuestionId());
            if (question == null) {
                continue;
            }

            QuizAnswer selectedAnswer = submitted.getSelectedAnswerId() != null
                    ? answerMap.get(submitted.getSelectedAnswerId())
                    : null;

            boolean isCorrect = selectedAnswer != null && selectedAnswer.isCorrect();
            if (isCorrect) {
                correctCount++;
            }

            // Luu ket qua tung cau
            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setQuizAttempt(attempt);
            attemptAnswer.setQuestion(question);
            attemptAnswer.setSelectedAnswer(selectedAnswer);
            attemptAnswer.setIsCorrect(isCorrect);
            attemptAnswers.add(attemptAnswer);

            QuizAnswer correctAnswer = correctAnswerByQuestion.get(question.getId());
            answerResults.add(AnswerResultResponse.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .selectedAnswerId(selectedAnswer != null ? selectedAnswer.getId() : null)
                    .selectedAnswerText(selectedAnswer != null ? selectedAnswer.getAnswerText() : null)
                    .correctAnswerId(correctAnswer != null ? correctAnswer.getId() : null)
                    .correctAnswerText(correctAnswer != null ? correctAnswer.getAnswerText() : null)
                    .isCorrect(isCorrect)
                    .build());
        }

        quizAttemptAnswerRepository.saveAll(attemptAnswers);

        // Tinh diem va thoi gian
        LocalDateTime endTime = LocalDateTime.now();
        int timeTakenSeconds = (int) ChronoUnit.SECONDS.between(attempt.getStartTime(), endTime);
        BigDecimal scorePercentage = questions.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount * 100.0 / questions.size()).setScale(2, RoundingMode.HALF_UP);
        boolean isPassed = scorePercentage.compareTo(BigDecimal.valueOf(quiz.getPassScorePercentage())) >= 0;

        // Xu ly coins cho school campaign
        Integer coinsEarned = null;
        if (isPassed && participant.getCampaign().getCampaignType() != CampaignType.PARTNERSHIP_EVENT
                && quiz.getCoinsOnPass() != null && quiz.getCoinsOnPass() > 0) {
            coinsEarned = quiz.getCoinsOnPass();
        }

        // Cap nhat attempt
        attempt.setEndTime(endTime);
        attempt.setTimeTakenSeconds(timeTakenSeconds);
        attempt.setCorrectAnswers(correctCount);
        attempt.setTotalQuestions(questions.size());
        attempt.setScorePercentage(scorePercentage);
        attempt.setPassed(isPassed);
        attempt.setCompleted(true);
        attempt.setCoinsEarned(coinsEarned);
        quizAttemptRepository.save(attempt);

                if (coinsEarned != null && coinsEarned > 0) {
                        awardQuizCoins(participant, attempt, coinsEarned);
                }

        // Cap nhat leaderboard
        updateLeaderboardAfterQuizSubmit(participant, round);

        // Lay diem cao nhat trong tat ca lan da lam
        BigDecimal bestScore = quizAttemptRepository
                .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                        participant.getId(), round.getId(), quiz.getId())
                .map(QuizAttempt::getScorePercentage)
                .orElse(scorePercentage);

        int attemptsUsed = quizAttemptRepository.countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
                participant.getId(), round.getId(), quiz.getId());

        CampaignRoundQuiz roundQuiz = campaignRoundQuizRepository
                .findByCampaignRoundIdAndQuizId(round.getId(), quiz.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình quiz trong round"));

        return QuizAttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .correctAnswers(correctCount)
                .totalQuestions(questions.size())
                .scorePercentage(scorePercentage)
                .timeTakenSeconds(timeTakenSeconds)
                .isPassed(isPassed)
                .coinsEarned(coinsEarned)
                .bestScorePercentage(bestScore)
                .attemptsUsed(attemptsUsed)
                .maxAttempts(roundQuiz.getMaxAttempts())
                .answerResults(answerResults)
                .build();
    }

    @Override
    public StudentRoundQuizProgressResponse getRoundQuizProgress(UUID campaignId, UUID roundId) {
        Student student = getCurrentStudent();

        // Xac thuc student la participant
        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia campaign này"));

        CampaignRound round = campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));

        // Lay tat ca quiz cua round
        List<CampaignRoundQuiz> roundQuizzes = campaignRoundQuizRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(roundId);

        int completedCount = 0;
        boolean allRequiredDone = true;
        List<QuizProgressItem> progressItems = new ArrayList<>();

        for (CampaignRoundQuiz rq : roundQuizzes) {
            Quiz quiz = rq.getQuiz();
            int attemptsUsed = quizAttemptRepository.countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
                    participant.getId(), roundId, quiz.getId());

            // Lay diem cao nhat
            Optional<QuizAttempt> bestAttempt = quizAttemptRepository
                    .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                            participant.getId(), roundId, quiz.getId());

            BigDecimal bestScore = bestAttempt.map(QuizAttempt::getScorePercentage).orElse(null);
            boolean isPassed = bestAttempt.map(QuizAttempt::isPassed).orElse(false);

            // Xac dinh status
            String status;
            if (attemptsUsed == 0) {
                // Kiem tra co attempt dang mo khong
                boolean hasOpenAttempt = quizAttemptRepository
                        .findByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedFalse(
                                participant.getId(), roundId, quiz.getId())
                        .isPresent();
                status = hasOpenAttempt ? "IN_PROGRESS" : "NOT_STARTED";
            } else if (isPassed) {
                status = "PASSED";
                completedCount++;
            } else if (attemptsUsed >= rq.getMaxAttempts()) {
                status = "FAILED";
                completedCount++;
            } else {
                // Con lan lam lai, chua pass
                boolean hasOpenAttempt = quizAttemptRepository
                        .findByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedFalse(
                                participant.getId(), roundId, quiz.getId())
                        .isPresent();
                status = hasOpenAttempt ? "IN_PROGRESS" : "NOT_STARTED";
            }

            // Neu quiz required ma chua pass thi khong du dieu kien ranking
            if (rq.isRequired() && !isPassed) {
                allRequiredDone = false;
            }

            progressItems.add(QuizProgressItem.builder()
                    .quizId(quiz.getId())
                    .quizTitle(quiz.getTitle())
                    .displayOrder(rq.getDisplayOrder())
                    .maxAttempts(rq.getMaxAttempts())
                    .attemptsUsed(attemptsUsed)
                    .bestScore(bestScore)
                    .isPassed(isPassed)
                    .status(status)
                    .build());
        }

        return StudentRoundQuizProgressResponse.builder()
                .roundId(round.getId())
                .roundName(round.getRoundName())
                .totalQuizzes(roundQuizzes.size())
                .completedQuizzes(completedCount)
                .isEligibleForRanking(allRequiredDone && !roundQuizzes.isEmpty())
                .quizzes(progressItems)
                .build();
    }

    @Override
    public QuizAttemptResultResponse getQuizAttemptResult(UUID attemptId) {
        Student student = getCurrentStudent();

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài làm"));

        if (!attempt.getCampaignParticipant().getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Bạn không có quyền xem bài làm này");
        }

        if (!attempt.isCompleted()) {
            throw new BadRequestException("Bài làm chưa được nộp");
        }

        // Lay dap an da chon cua student
        List<QuizAttemptAnswer> attemptAnswers = quizAttemptAnswerRepository.findByQuizAttemptId(attemptId);

        // Lay tat ca cau hoi de biet dap an dung
        List<QuizQuestion> questions = quizQuestionRepository
                .findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(attempt.getQuiz().getId());
        List<QuizAnswer> allAnswers = quizAnswerRepository.findByQuestionIn(questions);
        Map<UUID, QuizAnswer> correctAnswerByQuestion = allAnswers.stream()
                .filter(QuizAnswer::isCorrect)
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        List<AnswerResultResponse> answerResults = attemptAnswers.stream()
                .map(aa -> {
                    QuizAnswer correct = correctAnswerByQuestion.get(aa.getQuestion().getId());
                    return AnswerResultResponse.builder()
                            .questionId(aa.getQuestion().getId())
                            .questionText(aa.getQuestion().getQuestionText())
                            .selectedAnswerId(aa.getSelectedAnswer() != null ? aa.getSelectedAnswer().getId() : null)
                            .selectedAnswerText(aa.getSelectedAnswer() != null ? aa.getSelectedAnswer().getAnswerText() : null)
                            .correctAnswerId(correct != null ? correct.getId() : null)
                            .correctAnswerText(correct != null ? correct.getAnswerText() : null)
                            .isCorrect(Boolean.TRUE.equals(aa.getIsCorrect()))
                            .build();
                })
                .toList();

        CampaignParticipant participant = attempt.getCampaignParticipant();
        CampaignRound round = attempt.getCampaignRound();

        BigDecimal bestScore = quizAttemptRepository
                .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                        participant.getId(), round.getId(), attempt.getQuiz().getId())
                .map(QuizAttempt::getScorePercentage)
                .orElse(attempt.getScorePercentage());

        int attemptsUsed = quizAttemptRepository.countByCampaignParticipantIdAndCampaignRoundIdAndQuizId(
                participant.getId(), round.getId(), attempt.getQuiz().getId());

        CampaignRoundQuiz roundQuiz = campaignRoundQuizRepository
                .findByCampaignRoundIdAndQuizId(round.getId(), attempt.getQuiz().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình quiz trong round"));

        return QuizAttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .correctAnswers(attempt.getCorrectAnswers())
                .totalQuestions(attempt.getTotalQuestions())
                .scorePercentage(attempt.getScorePercentage())
                .timeTakenSeconds(attempt.getTimeTakenSeconds() != null ? attempt.getTimeTakenSeconds() : 0)
                .isPassed(attempt.isPassed())
                .coinsEarned(attempt.getCoinsEarned())
                .bestScorePercentage(bestScore)
                .attemptsUsed(attemptsUsed)
                .maxAttempts(roundQuiz.getMaxAttempts())
                .answerResults(answerResults)
                .build();
    }

    @Override
    public List<QuizAttemptSummaryResponse> getQuizAttemptHistory(UUID campaignId, UUID roundId, UUID quizId) {
        Student student = getCurrentStudent();

        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia campaign này"));

        // Kiem tra round thuoc campaign
        campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));

        // Kiem tra quiz thuoc round
        campaignRoundQuizRepository.findByCampaignRoundIdAndQuizId(roundId, quizId)
                .orElseThrow(() -> new NotFoundException("Quiz không thuộc round này"));

        List<QuizAttempt> attempts = quizAttemptRepository
                .findByCampaignParticipantIdAndCampaignRoundIdAndQuizId(participant.getId(), roundId, quizId);

        return attempts.stream()
                .map(a -> QuizAttemptSummaryResponse.builder()
                        .attemptId(a.getId())
                        .attemptNumber(a.getAttemptNumber())
                        .scorePercentage(a.getScorePercentage())
                        .timeTakenSeconds(a.getTimeTakenSeconds() != null ? a.getTimeTakenSeconds() : 0)
                        .isPassed(a.isPassed())
                        .build())
                .toList();
    }

    // Helper: cap nhat leaderboard sau khi student nop quiz
    private void updateLeaderboardAfterQuizSubmit(CampaignParticipant participant, CampaignRound round) {
        // Lay tat ca quiz required cua round
        List<CampaignRoundQuiz> roundQuizzes = campaignRoundQuizRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(round.getId());

        // Tinh diem trung binh quiz (dung diem cao nhat cua moi quiz)
        List<BigDecimal> bestScores = new ArrayList<>();
        List<BigDecimal> bestTimes = new ArrayList<>();

        for (CampaignRoundQuiz rq : roundQuizzes) {
            quizAttemptRepository
                    .findTopByCampaignParticipantIdAndCampaignRoundIdAndQuizIdAndIsCompletedTrueOrderByScorePercentageDesc(
                            participant.getId(), round.getId(), rq.getQuiz().getId())
                    .ifPresent(best -> {
                        bestScores.add(best.getScorePercentage());
                        if (best.getTimeTakenSeconds() != null) {
                            bestTimes.add(BigDecimal.valueOf(best.getTimeTakenSeconds()));
                        }
                    });
        }

        if (bestScores.isEmpty()) {
            return;
        }

        BigDecimal quizAccuracy = bestScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(bestScores.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgQuizTime = bestTimes.isEmpty() ? BigDecimal.ZERO
                : bestTimes.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(bestTimes.size()), 2, RoundingMode.HALF_UP);

        int quizzesCompleted = bestScores.size();
        int totalQuizCoins = quizAttemptRepository.findByCampaignParticipantIdAndCampaignRoundIdAndIsCompletedTrue(
                        participant.getId(), round.getId())
                .stream()
                .map(QuizAttempt::getCoinsEarned)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int totalGameCoins = gameSessionRepository.findCompletedByParticipantAndRound(participant.getId(), round.getId())
                .stream()
                .map(GameSession::getCoinAwarded)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        // Tim hoac tao RoundLeaderboard entry
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

        roundLb.setQuizAccuracyPercentage(quizAccuracy);
        roundLb.setQuizzesCompleted(quizzesCompleted);

        // combinedAccuracy = trung binh cua game va quiz (neu chua co game thi chi tinh quiz)
        BigDecimal gameAccuracy = roundLb.getGameAccuracyPercentage();
        BigDecimal combinedAccuracy;
        if (gameAccuracy != null) {
            combinedAccuracy = gameAccuracy.add(quizAccuracy)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            combinedAccuracy = quizAccuracy;
        }
        roundLb.setCombinedAccuracyPercentage(combinedAccuracy);

        // avgTimeSeconds = trung binh game + quiz
        BigDecimal gameAvgTime = roundLb.getAvgTimeSeconds();
        BigDecimal combinedTime;
        if (gameAvgTime != null && gameAvgTime.compareTo(BigDecimal.ZERO) > 0) {
            combinedTime = gameAvgTime.add(avgQuizTime)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            combinedTime = avgQuizTime;
        }
        roundLb.setAvgTimeSeconds(combinedTime);
                if (round.getCampaign().getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
                        roundLb.setTotalCoinsEarned(totalQuizCoins + totalGameCoins);
                }

        roundLeaderboardRepository.save(roundLb);

        // Re-rank tat ca entry trong round
        reRankRound(round.getId());

        // Neu la school campaign (1 round) thi cap nhat SchoolLeaderboard
        if (round.getCampaign().getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
                        updateSchoolLeaderboard(participant, round, quizAccuracy, avgQuizTime, quizzesCompleted, totalQuizCoins);
        }
    }

    // Re-rank tat ca student trong round theo combinedAccuracy DESC, avgTime ASC
    private void reRankRound(UUID roundId) {
        List<RoundLeaderboard> entries = roundLeaderboardRepository.findByCampaignRoundId(roundId);

        // Sort: combinedAccuracy DESC, avgTime ASC (null xu ly o cuoi)
        entries.sort(Comparator
                .comparing(RoundLeaderboard::getCombinedAccuracyPercentage,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RoundLeaderboard::getAvgTimeSeconds,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        // Gan rank tong the
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setOverallRankInRound(i + 1);
        }

        // Gan rank theo truong
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

    // Cap nhat SchoolLeaderboard (danh cho school campaign - 1 round)
        private void updateSchoolLeaderboard(CampaignParticipant participant, CampaignRound round,
                                                                                 BigDecimal quizAccuracy, BigDecimal avgQuizTime, int quizzesCompleted,
                                                                                 int totalQuizCoins) {
                int totalGameCoins = gameSessionRepository
                        .findCompletedByParticipantAndCampaign(participant.getId(), round.getCampaign().getId())
                        .stream()
                        .map(GameSession::getCoinAwarded)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();

        SchoolLeaderboard schoolLb = schoolLeaderboardRepository
                .findByCampaignIdAndStudentId(round.getCampaign().getId(), participant.getStudent().getId())
                .orElseGet(() -> {
                    SchoolLeaderboard newEntry = new SchoolLeaderboard();
                    newEntry.setCampaign(round.getCampaign());
                    newEntry.setStudent(participant.getStudent());
                    newEntry.setSchool(participant.getSchool());
                    return newEntry;
                });

        schoolLb.setQuizAccuracyPercentage(quizAccuracy);
        schoolLb.setQuizzesCompleted(quizzesCompleted);

        BigDecimal gameAccuracy = schoolLb.getGameAccuracyPercentage();
        BigDecimal combinedAccuracy = gameAccuracy != null
                ? gameAccuracy.add(quizAccuracy).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                : quizAccuracy;
        schoolLb.setCombinedAccuracyPercentage(combinedAccuracy);

        BigDecimal gameAvgTime = schoolLb.getAvgTimeSeconds();
        BigDecimal combinedTime = (gameAvgTime != null && gameAvgTime.compareTo(BigDecimal.ZERO) > 0)
                ? gameAvgTime.add(avgQuizTime).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                : avgQuizTime;
        schoolLb.setAvgTimeSeconds(combinedTime);
        schoolLb.setTotalCoinsEarned(totalQuizCoins + totalGameCoins);

        schoolLeaderboardRepository.save(schoolLb);

        // Re-rank tat ca student trong campaign
        reRankSchoolCampaign(round.getCampaign().getId());
    }

    // Re-rank tat ca student trong school campaign
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

        private void ensurePartnershipRoundAccess(CampaignParticipant participant, CampaignRound requestedRound) {
                Campaign campaign = participant.getCampaign();
                if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
                        return;
                }

                LocalDateTime now = LocalDateTime.now();
                CampaignRound activeRound = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId()).stream()
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
                                participant.getStudent().getId()
                ) || campaignRoundParticipantRepository.existsByCampaignRoundIdAndCampaignParticipantIdAndIsAdvancedTrue(
                                previousRound.getId(),
                                participant.getId()
                );

                if (!advanced) {
                        throw new BadRequestException("Bạn không đủ điều kiện tham gia round này");
                }
        }

        private void awardQuizCoins(CampaignParticipant participant, QuizAttempt attempt, int coinsEarned) {
                if (participant.getCampaign().getCampaignType() != CampaignType.SCHOOL_INTERNAL) {
                        return;
                }

                Student student = participant.getStudent();
                BigDecimal before = student.getTotalCoins() == null ? BigDecimal.ZERO : student.getTotalCoins();
                BigDecimal delta = BigDecimal.valueOf(coinsEarned);
                BigDecimal after = before.add(delta);

                student.setTotalCoins(after);
                studentRepository.save(student);

                CoinTransaction tx = new CoinTransaction();
                tx.setTransactionCode("TX-QUIZ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                tx.setStudent(student);
                tx.setCampaign(participant.getCampaign());
                tx.setTransactionType(TransactionType.EARN_QUIZ);
                tx.setAmount(delta);
                tx.setBalanceBefore(before);
                tx.setBalanceAfter(after);
                tx.setReferenceType("QUIZ_ATTEMPT");
                tx.setReferenceId(attempt.getId());
                tx.setDescription("Thưởng xu từ quiz " + attempt.getQuiz().getTitle());
                tx.setCreatedBy(student.getUser());
                coinTransactionRepository.save(tx);
        }
}
