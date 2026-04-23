package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.ConfirmAiQuizRequest;
import com.sep490.ecoverse_be.dto.request.GenerateAiQuizRequest;
import com.sep490.ecoverse_be.dto.request.QuizAnswerRequest;
import com.sep490.ecoverse_be.dto.request.QuizQuestionRequest;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IAiQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiQuizServiceImpl implements IAiQuizService {

    private final GeminiService geminiService;
    private final DocumentRagService documentRagService;
    private final CampaignRepository campaignRepository;
    private final CampaignRoundRepository campaignRoundRepository;
    private final RoundGameConfigRepository roundGameConfigRepository;
    private final WasteItemRepository wasteItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SchoolRepository schoolRepository;
    private final PartnershipRepository partnershipRepository;

    // ── System Prompt ────────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
            You are an expert educational quiz designer specializing in environmental education, \
            waste management, and recycling awareness for K-12 students.

            Your task is to generate multiple-choice quiz questions that are:
            - Accurate, age-appropriate, and educationally valuable
            - Directly grounded in the provided source content (campaign content, uploaded files, and waste item data)
            - Varied in cognitive demand: mix recall, comprehension, and application questions
            - Clear, unambiguous, and free of trick questions

            Grade-level difficulty guidelines:
            - Grade 1–2: Simple vocabulary, one-concept questions, 3 answer choices
            - Grade 3–5: Slightly complex sentences, cause-effect understanding, 4 answer choices
            - Grade 6–8: Multi-concept questions, inference required, 4 answer choices
            - Grade 9–12: Critical thinking, data interpretation, real-world application, 4 answer choices

            Question distribution rules:
            - At least 40% of questions must be directly derived from waste item data (description, \
              recyclingTips, decompositionTime, funFact, category, subCategory, itemName)
            - At least 30% from campaign content
            - Remaining questions may draw from imported file content (if provided)
            - Ensure topic diversity — do not repeat the same waste item in more than 2 questions

            Answer design rules:
            - Each question must have exactly 1 correct answer
            - Distractors (wrong answers) must be plausible but clearly incorrect upon reflection
            - Avoid "All of the above" or "None of the above"
            - Randomize the position of the correct answer across questions

            Output format:
            Respond ONLY with a valid JSON array. No explanation, no markdown fences, no preamble.
            Each element must conform exactly to the output schema provided.""";

    // ── Generate AI Quiz ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public AiQuizPreviewResponse generateAiQuiz(GenerateAiQuizRequest request) {
        User currentUser = getCurrentUser();

        // 1. Resolve School/Partnership
        School school = null;
        Partnership partnership = null;
        Subscription activeSubscription;

        if (currentUser.getRole() == Role.PARTNERSHIP_SCHOOL) {
            school = resolveSchool(currentUser.getId());
            activeSubscription = subscriptionRepository
                    .findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE)
                    .orElse(null);
            if (activeSubscription == null) {
                activeSubscription = subscriptionRepository
                        .findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.PENDING_RENEWAL)
                        .orElse(null);
            }
        } else {
            partnership = resolvePartnership(currentUser.getId());
            activeSubscription = subscriptionRepository
                    .findByPartnershipIdAndStatus(partnership.getId(), SubscriptionStatus.ACTIVE)
                    .orElse(null);
            if (activeSubscription == null) {
                activeSubscription = subscriptionRepository
                        .findByPartnershipIdAndStatus(partnership.getId(), SubscriptionStatus.PENDING_RENEWAL)
                        .orElse(null);
            }
        }

        if (activeSubscription == null) {
            throw new BadRequestException("Tài khoản chưa có gói subscription hợp lệ.");
        }

        // 2. Check AI quota
        checkAiQuota(school, partnership, activeSubscription);

        // 3. Lấy campaign info
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chiến dịch"));

        // 4. Lấy round + game config → waste items
        CampaignRound round = campaignRoundRepository.findByIdAndCampaignId(
                request.getRoundId(), request.getCampaignId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy round trong chiến dịch này"));

        List<WasteItem> wasteItems = getWasteItemsFromRound(round);
        if (wasteItems.isEmpty()) {
            throw new BadRequestException(
                    "Round chưa có waste items nào. Vui lòng cấu hình game trước khi tạo quiz AI.");
        }

        // 5. RAG: lấy nội dung liên quan từ file (nếu có)
        String ragQuery = buildRagQuery(campaign, wasteItems);
        String fileContent = documentRagService.getRelevantContent(request.getFileIds(), ragQuery);

        // 6. Build user prompt
        String userPrompt = buildUserPrompt(campaign, wasteItems, fileContent,
                request.getQuestionCount(), request.getTargetGrade());

        // 7. Gọi Gemini AI
        log.info("Gọi Gemini AI để tạo {} câu hỏi cho campaign '{}'",
                request.getQuestionCount(), campaign.getCampaignName());

        List<GeminiService.GeminiQuizQuestion> generatedQuestions = geminiService.generateQuizQuestions(SYSTEM_PROMPT,
                userPrompt);

        // 8. Tạo AiGenerationLog — trừ quota ngay
        AiGenerationLog genLog = new AiGenerationLog();
        genLog.setSchool(school);
        genLog.setPartnership(partnership);
        genLog.setTargetGrade(request.getTargetGrade());
        genLog.setRequestedQuestionCount(request.getQuestionCount());
        genLog.setQuestionsGenerated(generatedQuestions.size());
        genLog.setAiProvider("Google");
        genLog.setAiModel("gemini-2.0-flash");
        genLog.setPromptUsed(userPrompt);
        genLog.setStatus("SUCCESS");
        genLog.setUsageCharged(true); // trừ quota ngay
        genLog.setUsageChargedAt(OffsetDateTime.now());
        genLog.setCreatedBy(currentUser);

        // Lưu waste item context snapshot
        Map<String, Object> contextSnapshot = new LinkedHashMap<>();
        contextSnapshot.put("wasteItemIds",
                wasteItems.stream().map(w -> w.getId().toString()).toList());
        contextSnapshot.put("campaignId", campaign.getId().toString());
        contextSnapshot.put("roundId", round.getId().toString());
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            contextSnapshot.put("fileIds",
                    request.getFileIds().stream().map(UUID::toString).toList());
        }
        genLog.setWasteItemContextSnapshot(contextSnapshot);

        genLog = aiGenerationLogRepository.save(genLog);

        // 9. Build preview response (QuizResponse format, chưa lưu DB)
        QuizResponse quizPreview = buildQuizPreview(
                generatedQuestions, request, campaign);

        return AiQuizPreviewResponse.builder()
                .aiGenerationLogId(genLog.getId())
                .quizPreview(quizPreview)
                .build();
    }

    // ── Confirm AI Quiz ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponse confirmAiQuiz(ConfirmAiQuizRequest request) {
        User currentUser = getCurrentUser();

        // 1. Tìm AiGenerationLog
        AiGenerationLog genLog = aiGenerationLogRepository.findById(request.getAiGenerationLogId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Generation Log"));

        // 2. Validate ownership
        validateLogOwnership(genLog, currentUser);

        // 3. Kiểm tra log đã được confirm chưa
        if (genLog.getQuiz() != null) {
            throw new BadRequestException("Quiz AI này đã được lưu trước đó");
        }

        // 4. Resolve School/Partnership
        School school = null;
        Partnership partnership = null;
        if (currentUser.getRole() == Role.PARTNERSHIP_SCHOOL) {
            school = resolveSchool(currentUser.getId());
        } else {
            partnership = resolvePartnership(currentUser.getId());
        }

        // 5. Determine difficulty
        QuizDifficulty difficulty = determineDifficulty(genLog.getTargetGrade());

        // 6. Tạo Quiz entity
        Quiz quiz = new Quiz();
        quiz.setTitle("AI Quiz — " + (genLog.getTargetGrade() != null
                ? "Lớp " + genLog.getTargetGrade()
                : "General"));
        quiz.setDescription("Quiz được tạo bởi AI dựa trên nội dung chiến dịch và waste items");
        quiz.setDifficulty(difficulty);
        quiz.setSource(QuizSource.AI_GENERATED);
        quiz.setCreatedBy(QuizCreated.AI);
        quiz.setTargetGrade(genLog.getTargetGrade());
        quiz.setQuestionCount(request.getQuestions().size());
        quiz.setTimePerQuestion(30); // default
        quiz.setPassScorePercentage(70);
        quiz.setCoinsOnPass(10);
        quiz.setPublished(false);
        quiz.setActive(true);
        quiz.setSchool(school);
        quiz.setPartnership(partnership);

        // Nếu có round game config info trong log
        if (genLog.getRoundGameConfig() != null) {
            quiz.setGeneratedForRoundGameConfig(genLog.getRoundGameConfig());
        }

        quiz = quizRepository.save(quiz);

        // 7. Tạo questions + answers
        saveQuestionsAndAnswers(quiz, request.getQuestions());

        // 8. Link log → quiz
        genLog.setQuiz(quiz);
        aiGenerationLogRepository.save(genLog);

        // 9. Trả QuizResponse
        return mapToQuizResponse(quiz);
    }

    // ── Build User Prompt ────────────────────────────────────────────────────

    private String buildUserPrompt(Campaign campaign, List<WasteItem> wasteItems,
            String fileContent, int questionCount, int targetGrade) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format(
                "Generate exactly %d quiz questions for Grade %d students.\n\n", questionCount, targetGrade));

        // Campaign context
        prompt.append("=== CAMPAIGN CONTENT ===\n");
        prompt.append("Campaign name: ").append(campaign.getCampaignName()).append("\n");
        if (campaign.getDescription() != null && !campaign.getDescription().isBlank()) {
            prompt.append("Description: ").append(campaign.getDescription()).append("\n");
        }
        prompt.append("\n");

        // Waste items context
        prompt.append("=== WASTE ITEM DATA ===\n");
        for (WasteItem item : wasteItems) {
            prompt.append("- Item: ").append(item.getItemName());
            prompt.append(" | Category: ").append(item.getCategory());
            if (item.getSubCategory() != null) {
                prompt.append(" | SubCategory: ").append(item.getSubCategory().getDisplayName());
            }
            if (item.getDescription() != null) {
                prompt.append(" | Description: ").append(item.getDescription());
            }
            if (item.getRecyclingTips() != null) {
                prompt.append(" | RecyclingTips: ").append(item.getRecyclingTips());
            }
            if (item.getDecompositionTime() != null) {
                prompt.append(" | DecompositionTime: ").append(item.getDecompositionTime());
            }
            if (item.getFunFact() != null) {
                prompt.append(" | FunFact: ").append(item.getFunFact());
            }
            prompt.append("\n");
        }
        prompt.append("\n");

        // File content (RAG-processed)
        if (fileContent != null && !fileContent.isBlank()) {
            prompt.append("=== SUPPLEMENTARY DOCUMENT CONTENT (from uploaded files) ===\n");
            prompt.append(fileContent);
            prompt.append("\n\n");
        }

        // Output schema
        prompt.append("=== OUTPUT SCHEMA ===\n");
        prompt.append("""
                Return a JSON array where each element has this structure:
                {
                  "questionText": "string — the question text",
                  "answers": [
                    { "answerText": "string — answer option", "correct": boolean },
                    { "answerText": "string — answer option", "correct": boolean },
                    { "answerText": "string — answer option", "correct": boolean },
                    { "answerText": "string — answer option", "correct": boolean }
                  ]
                }

                Rules:
                - Exactly 1 correct answer per question (correct=true)
                - 3–4 answer options per question depending on grade level
                - Questions must be in Vietnamese
                """);

        return prompt.toString();
    }

    // ── Build RAG Query ──────────────────────────────────────────────────────

    private String buildRagQuery(Campaign campaign, List<WasteItem> wasteItems) {
        StringBuilder query = new StringBuilder();
        query.append(campaign.getCampaignName()).append(" ");
        if (campaign.getDescription() != null) {
            query.append(campaign.getDescription()).append(" ");
        }
        for (WasteItem item : wasteItems) {
            query.append(item.getItemName()).append(" ");
            if (item.getCategory() != null) {
                query.append(item.getCategory().name()).append(" ");
            }
        }
        return query.toString().trim();
    }

    // ── Get Waste Items from Round ───────────────────────────────────────────

    private List<WasteItem> getWasteItemsFromRound(CampaignRound round) {
        List<RoundGameConfig> gameConfigs = roundGameConfigRepository
                .findByCampaignRoundIdOrderByDisplayOrderAsc(round.getId());

        if (gameConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect tất cả sub-category IDs từ preset config
        Set<UUID> allSubCategoryIds = new HashSet<>();
        for (RoundGameConfig config : gameConfigs) {
            if (config.getPresetSubCategoryConfig() != null) {
                for (List<UUID> subCatIds : config.getPresetSubCategoryConfig().values()) {
                    allSubCategoryIds.addAll(subCatIds);
                }
            }
        }

        if (allSubCategoryIds.isEmpty()) {
            // Fallback: lấy waste items theo categories từ preset items
            return getWasteItemsFromPresets(gameConfigs);
        }

        return wasteItemRepository.findBySubCategoryIdInAndIsDeleteFalseAndIsActiveTrue(
                new ArrayList<>(allSubCategoryIds));
    }

    private List<WasteItem> getWasteItemsFromPresets(List<RoundGameConfig> gameConfigs) {
        Set<WasteCategory> categories = new HashSet<>();

        for (RoundGameConfig config : gameConfigs) {
            if (config.getSelectedPresets() != null) {
                for (GameLevelPreset preset : config.getSelectedPresets()) {
                    if (preset.getItems() != null) {
                        for (GameLevelPresetItem item : preset.getItems()) {
                            if (item.getWasteCategories() != null) {
                                categories.addAll(item.getWasteCategories());
                            }
                        }
                    }
                }
            }
        }

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        return wasteItemRepository.findByCategoryInAndIsDeleteFalseAndIsActiveTrue(
                new ArrayList<>(categories));
    }

    // ── Build Quiz Preview ───────────────────────────────────────────────────

    private QuizResponse buildQuizPreview(
            List<GeminiService.GeminiQuizQuestion> generated,
            GenerateAiQuizRequest request,
            Campaign campaign) {

        QuizDifficulty difficulty = determineDifficulty(request.getTargetGrade());

        List<QuizQuestionResponse> questionResponses = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            GeminiService.GeminiQuizQuestion gq = generated.get(i);

            List<QuizAnswerResponse> answerResponses = new ArrayList<>();
            if (gq.getAnswers() != null) {
                for (GeminiService.GeminiQuizAnswer ga : gq.getAnswers()) {
                    answerResponses.add(QuizAnswerResponse.builder()
                            .id(null)
                            .answerText(ga.getAnswerText())
                            .correct(ga.isCorrect())
                            .build());
                }
            }

            questionResponses.add(QuizQuestionResponse.builder()
                    .id(null)
                    .questionOrder(i + 1)
                    .questionText(gq.getQuestionText())
                    .questionImageUrl(null)
                    .questionImagePresignedUrl(null)
                    .answers(answerResponses)
                    .build());
        }

        return QuizResponse.builder()
                .id(null) // chưa lưu DB
                .title("AI Quiz — " + campaign.getCampaignName() + " — Lớp " + request.getTargetGrade())
                .description("Quiz được tạo bởi AI dựa trên nội dung chiến dịch '"
                        + campaign.getCampaignName() + "' và waste items")
                .difficulty(difficulty)
                .source(QuizSource.AI_GENERATED)
                .createdBy(QuizCreated.AI)
                .targetGrade(request.getTargetGrade())
                .coinsOnPass(request.getCoinsOnPass())
                .timePerQuestion(request.getTimePerQuestion())
                .passScorePercentage(70)
                .isPublished(false)
                .isActive(true)
                .createdAt(null)
                .updatedAt(null)
                .questions(questionResponses)
                .build();
    }

    // ── Quota Check ──────────────────────────────────────────────────────────

    private void checkAiQuota(School school, Partnership partnership, Subscription subscription) {
        SubscriptionPlan plan = subscription.getPlan();

        // maxAiQuizGenerations == null → unlimited
        if (plan.getMaxAiQuizGenerations() == null) {
            return;
        }

        // maxAiQuizGenerations == 0 → feature disabled
        if (plan.getMaxAiQuizGenerations() == 0) {
            throw new BadRequestException(
                    "Gói subscription hiện tại không hỗ trợ tính năng tạo quiz AI. "
                            + "Vui lòng nâng cấp gói.");
        }

        long usedCount;
        if (school != null) {
            usedCount = aiGenerationLogRepository.countChargedBySchoolIdInPeriod(
                    school.getId(), subscription.getStartDate(), subscription.getEndDate());
        } else {
            usedCount = aiGenerationLogRepository.countChargedByPartnershipIdInPeriod(
                    partnership.getId(), subscription.getStartDate(), subscription.getEndDate());
        }

        if (usedCount >= plan.getMaxAiQuizGenerations()) {
            throw new BadRequestException(String.format(
                    "Đã hết quota AI quiz generation (%d/%d). Vui lòng nâng cấp gói hoặc chờ kỳ tiếp theo.",
                    usedCount, plan.getMaxAiQuizGenerations()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUser();
    }

    private School resolveSchool(UUID userId) {
        return schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));
    }

    private Partnership resolvePartnership(UUID userId) {
        return partnershipRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin tổ chức đối tác"));
    }

    private void validateLogOwnership(AiGenerationLog log, User user) {
        if (user.getRole() == Role.PARTNERSHIP_SCHOOL) {
            School school = resolveSchool(user.getId());
            if (log.getSchool() == null || !log.getSchool().getId().equals(school.getId())) {
                throw new BadRequestException("Bạn không có quyền truy cập AI Generation Log này");
            }
        } else {
            Partnership partnership = resolvePartnership(user.getId());
            if (log.getPartnership() == null || !log.getPartnership().getId().equals(partnership.getId())) {
                throw new BadRequestException("Bạn không có quyền truy cập AI Generation Log này");
            }
        }
    }

    private QuizDifficulty determineDifficulty(Integer targetGrade) {
        if (targetGrade == null)
            return QuizDifficulty.MEDIUM;
        if (targetGrade <= 5)
            return QuizDifficulty.EASY;
        if (targetGrade <= 8)
            return QuizDifficulty.MEDIUM;
        return QuizDifficulty.HARD;
    }

    private void saveQuestionsAndAnswers(Quiz quiz, List<QuizQuestionRequest> questionRequests) {
        Set<Integer> usedOrders = new HashSet<>();

        for (QuizQuestionRequest qReq : questionRequests) {
            if (!usedOrders.add(qReq.getQuestionOrder())) {
                throw new BadRequestException(
                        "Thứ tự câu hỏi " + qReq.getQuestionOrder() + " bị trùng lặp");
            }

            // Validate có ít nhất 1 đáp án đúng
            boolean hasCorrect = qReq.getAnswers().stream().anyMatch(QuizAnswerRequest::isCorrect);
            if (!hasCorrect) {
                throw new BadRequestException(
                        "Câu hỏi thứ tự " + qReq.getQuestionOrder() + " phải có ít nhất 1 đáp án đúng");
            }

            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setQuestionOrder(qReq.getQuestionOrder());
            question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
            question.setQuestionText(qReq.getQuestionText());
            question = quizQuestionRepository.save(question);

            for (QuizAnswerRequest aReq : qReq.getAnswers()) {
                QuizAnswer answer = new QuizAnswer();
                answer.setQuestion(question);
                answer.setAnswerText(aReq.getAnswerText());
                answer.setCorrect(aReq.isCorrect());
                quizAnswerRepository.save(answer);
            }
        }
    }

    private QuizResponse mapToQuizResponse(Quiz quiz) {
        List<QuizQuestion> questions = quizQuestionRepository
                .findByQuizIdAndIsActiveTrueOrderByQuestionOrder(quiz.getId());
        List<QuizAnswer> allAnswers = quizAnswerRepository.findByQuestionIn(questions);

        Map<UUID, List<QuizAnswer>> answersByQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        List<QuizQuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    List<QuizAnswerResponse> answerResponses = answersByQuestion
                            .getOrDefault(q.getId(), List.of())
                            .stream()
                            .map(a -> QuizAnswerResponse.builder()
                                    .id(a.getId())
                                    .answerText(a.getAnswerText())
                                    .correct(a.isCorrect())
                                    .build())
                            .collect(Collectors.toList());

                    return QuizQuestionResponse.builder()
                            .id(q.getId())
                            .questionOrder(q.getQuestionOrder())
                            .questionText(q.getQuestionText())
                            .questionImageUrl(q.getQuestionImageUrl())
                            .answers(answerResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .difficulty(quiz.getDifficulty())
                .source(quiz.getSource())
                .createdBy(quiz.getCreatedBy())
                .targetGrade(quiz.getTargetGrade())
                .coinsOnPass(quiz.getCoinsOnPass())
                .timePerQuestion(quiz.getTimePerQuestion())
                .passScorePercentage(quiz.getPassScorePercentage())
                .isPublished(quiz.isPublished())
                .isActive(quiz.isActive())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .questions(questionResponses)
                .build();
    }
}
