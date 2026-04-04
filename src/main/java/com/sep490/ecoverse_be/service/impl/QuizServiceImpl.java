package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.QuizCreated;
import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.QuizSource;
import com.sep490.ecoverse_be.enums.QuizType;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.QuizAnswerRepository;
import com.sep490.ecoverse_be.repository.QuizQuestionRepository;
import com.sep490.ecoverse_be.repository.QuizRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.service.IQuizService;
import com.sep490.ecoverse_be.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements IQuizService {

    private static final String[] EXCEL_HEADERS = {
            "quiz_title", "description", "difficulty", "target_grade", "quiz_type",
            "question_order", "question_text",
            "answer_A", "answer_B", "answer_C", "answer_D",
            "correct_answer", "coins_on_pass", "time_per_question", "pass_score_percentage"
    };

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;


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

    /**
     * Kiểm tra quyền sở hữu quiz dựa trên school hoặc partnership của user hiện tại.
     * PARTNERSHIP_SCHOOL -> dùng school_id, còn lại -> dùng partnership_id.
     */
    private Quiz assertOwnership(UUID quizId, User user) {
        if (user.getRole() == Role.PARTNERSHIP_SCHOOL) {
            School school = resolveSchool(user.getId());
            return quizRepository.findByIdAndSchoolIdAndIsActiveTrue(quizId, school.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz"));
        } else {
            Partnership partnership = resolvePartnership(user.getId());
            return quizRepository.findByIdAndPartnershipIdAndIsActiveTrue(quizId, partnership.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz"));
        }
    }


    private QuizAnswerResponse mapAnswer(QuizAnswer answer) {
        return QuizAnswerResponse.builder()
                .id(answer.getId())
                .answerText(answer.getAnswerText())
                .correct(answer.isCorrect())
                .build();
    }

    private QuizQuestionResponse mapQuestion(QuizQuestion question, List<QuizAnswer> answers) {
        List<QuizAnswerResponse> answerResponses = answers.stream()
                .map(this::mapAnswer)
                .collect(Collectors.toList());

        return QuizQuestionResponse.builder()
                .id(question.getId())
                .questionOrder(question.getQuestionOrder())
                .questionText(question.getQuestionText())
                .questionImageUrl(question.getQuestionImageUrl())
                .questionImagePresignedUrl(s3PresignedUrlService.generatePresignedUrl(question.getQuestionImageUrl()))
                .answers(answerResponses)
                .build();
    }

    private QuizResponse mapToQuizResponse(Quiz quiz) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(quiz.getId());
        List<QuizAnswer> allAnswers = quizAnswerRepository.findByQuestionIn(questions);

        Map<UUID, List<QuizAnswer>> answersByQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        List<QuizQuestionResponse> questionResponses = questions.stream()
                .map(q -> mapQuestion(q, answersByQuestion.getOrDefault(q.getId(), List.of())))
                .collect(Collectors.toList());

        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .difficulty(quiz.getDifficulty())
                .quizType(quiz.getQuizType())
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

    private QuizSummaryResponse mapToSummary(Quiz quiz, int questionCount) {
        return QuizSummaryResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .difficulty(quiz.getDifficulty())
                .quizType(quiz.getQuizType())
                .source(quiz.getSource())
                .createdBy(quiz.getCreatedBy())
                .targetGrade(quiz.getTargetGrade())
                .coinsOnPass(quiz.getCoinsOnPass())
                .timePerQuestion(quiz.getTimePerQuestion())
                .passScorePercentage(quiz.getPassScorePercentage())
                .isPublished(quiz.isPublished())
                .isActive(quiz.isActive())
                .questionCount(questionCount)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }

    /**
     * Build và lưu một Quiz mới.
     * quizCreated: USER cho tạo thủ công, IMPORT cho import Excel, AI cho AI generate.
     */
    private Quiz buildAndSaveQuiz(String title, String description, QuizDifficulty difficulty,
                                   QuizType quizType, Integer targetGrade, Integer coinOnPass,
                                   Integer timePerQuestion, Integer passScorePercentage,
                                   QuizCreated quizCreated, School school, Partnership partnership) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setDifficulty(difficulty);
        quiz.setQuizType(quizType);
        quiz.setSource(QuizSource.MANUAL);
        quiz.setTargetGrade(targetGrade);
        quiz.setCoinsOnPass(coinOnPass != null ? coinOnPass : 10);
        quiz.setTimePerQuestion(timePerQuestion);
        quiz.setPassScorePercentage(passScorePercentage != null ? passScorePercentage : 80);
        quiz.setPublished(false);
        quiz.setActive(true);
        quiz.setCreatedBy(quizCreated);
        quiz.setSchool(school);
        quiz.setPartnership(partnership);
        return quizRepository.save(quiz);
    }

    private void saveQuestionsAndAnswers(Quiz quiz, List<QuizQuestionRequest> questionRequests) {
        validateQuestionsHaveCorrectAnswer(questionRequests);

        Set<Integer> usedOrders = new HashSet<>();
        for (QuizQuestionRequest qReq : questionRequests) {
            if (!usedOrders.add(qReq.getQuestionOrder())) {
                throw new BadRequestException(
                        "Thứ tự câu hỏi " + qReq.getQuestionOrder() + " bị trùng lặp");
            }

            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setQuestionOrder(qReq.getQuestionOrder());
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

    private void validateQuestionsHaveCorrectAnswer(List<QuizQuestionRequest> questions) {
        for (QuizQuestionRequest q : questions) {
            boolean hasCorrect = q.getAnswers().stream().anyMatch(QuizAnswerRequest::isCorrect);
            if (!hasCorrect) {
                throw new BadRequestException(
                        "Câu hỏi thứ tự " + q.getQuestionOrder() + " phải có ít nhất 1 đáp án đúng");
            }
        }
    }


    @Override
    @Transactional
    public QuizResponse createQuizManual(CreateQuizRequest request) {
        User currentUser = getCurrentUser();

        School school = null;
        Partnership partnership = null;

        if (currentUser.getRole() == Role.PARTNERSHIP_SCHOOL) {
            school = resolveSchool(currentUser.getId());
        } else {
            partnership = resolvePartnership(currentUser.getId());
        }

        Quiz quiz = buildAndSaveQuiz(
                request.getTitle(), request.getDescription(),
                request.getDifficulty(), request.getQuizType(),
                request.getTargetGrade(), request.getCoinOnPass(),
                request.getTimePerQuestion(), request.getPassScorePercentage(),
                QuizCreated.USER, school, partnership
        );

        saveQuestionsAndAnswers(quiz, request.getQuestions());

        return mapToQuizResponse(quiz);
    }

    @Override
    public List<QuizSummaryResponse> getMyQuizzes() {
        User currentUser = getCurrentUser();
        List<Quiz> quizzes;

        if (currentUser.getRole() == Role.PARTNERSHIP_SCHOOL) {
            School school = resolveSchool(currentUser.getId());
            quizzes = quizRepository.findBySchoolIdAndIsActiveTrueOrderByCreatedAtDesc(school.getId());
        } else {
            Partnership partnership = resolvePartnership(currentUser.getId());
            quizzes = quizRepository.findByPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(partnership.getId());
        }

        return quizzes.stream()
                .map(q -> mapToSummary(q, quizQuestionRepository.countByQuizIdAndIsDeleteFalse(q.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public QuizResponse getQuizById(UUID quizId) {
        User currentUser = getCurrentUser();
        Quiz quiz = assertOwnership(quizId, currentUser);
        return mapToQuizResponse(quiz);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(UUID quizId, UpdateQuizRequest request) {
        User currentUser = getCurrentUser();
        Quiz quiz = assertOwnership(quizId, currentUser);

        if (quiz.isPublished()) {
            throw new BadRequestException("Không thể sửa quiz đã được publish. Vui lòng unpublish trước.");
        }

        if (request.getTitle() != null) quiz.setTitle(request.getTitle());
        if (request.getDescription() != null) quiz.setDescription(request.getDescription());
        if (request.getDifficulty() != null) quiz.setDifficulty(request.getDifficulty());
        if (request.getQuizType() != null) quiz.setQuizType(request.getQuizType());
        if (request.getTargetGrade() != null) quiz.setTargetGrade(request.getTargetGrade());
        if (request.getCoinOnPass() != null) quiz.setCoinsOnPass(request.getCoinOnPass());
        if (request.getTimePerQuestion() != null) quiz.setTimePerQuestion(request.getTimePerQuestion());
        if (request.getPassScorePercentage() != null) quiz.setPassScorePercentage(request.getPassScorePercentage());

        quizRepository.save(quiz);
        return mapToQuizResponse(quiz);
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID quizId) {
        User currentUser = getCurrentUser();
        Quiz quiz = assertOwnership(quizId, currentUser);

        quiz.setActive(false);
        quiz.setPublished(false);
        quizRepository.save(quiz);

        List<QuizQuestion> activeQuestions = quizQuestionRepository
                .findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(quiz.getId());
        for (QuizQuestion question : activeQuestions) {
            question.setDelete(true);
            quizQuestionRepository.save(question);
        }
    }

    @Override
    @Transactional
    public QuizResponse togglePublish(UUID quizId) {
        User currentUser = getCurrentUser();
        Quiz quiz = assertOwnership(quizId, currentUser);

        if (!quiz.isPublished()) {
            int questionCount = quizQuestionRepository.countByQuizIdAndIsDeleteFalse(quiz.getId());
            if (questionCount == 0) {
                throw new BadRequestException("Quiz phải có ít nhất 1 câu hỏi mới được publish");
            }
            quiz.setQuestionCount(questionCount);
        }

        quiz.setPublished(!quiz.isPublished());
        quizRepository.save(quiz);
        return mapToQuizResponse(quiz);
    }

    @Override
    @Transactional
    public QuizResponse addQuestions(UUID quizId, List<QuizQuestionRequest> questions) {
        User currentUser = getCurrentUser();
        Quiz quiz = assertOwnership(quizId, currentUser);

        if (quiz.isPublished()) {
            throw new BadRequestException("Không thể thêm câu hỏi vào quiz đã được publish. Vui lòng unpublish trước.");
        }

        Set<Integer> existingOrders = quizQuestionRepository
                .findByQuizIdAndIsDeleteFalseOrderByQuestionOrder(quiz.getId())
                .stream()
                .map(QuizQuestion::getQuestionOrder)
                .collect(Collectors.toSet());

        for (QuizQuestionRequest q : questions) {
            if (existingOrders.contains(q.getQuestionOrder())) {
                throw new BadRequestException(
                        "Thứ tự câu hỏi " + q.getQuestionOrder() + " đã tồn tại trong quiz này");
            }
        }

        saveQuestionsAndAnswers(quiz, questions);
        return mapToQuizResponse(quiz);
    }

    @Override
    @Transactional
    public QuizResponse updateQuestion(UUID quizId, UUID questionId, QuizQuestionRequest request) {
        User currentUser = getCurrentUser();
        assertOwnership(quizId, currentUser);

        QuizQuestion question = quizQuestionRepository.findByIdAndQuizIdAndIsDeleteFalse(questionId, quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong quiz này"));

        boolean orderChanged = question.getQuestionOrder() != request.getQuestionOrder();
        if (orderChanged && quizQuestionRepository.existsByQuizIdAndQuestionOrderAndIsDeleteFalse(quizId, request.getQuestionOrder())) {
            throw new BadRequestException(
                    "Thứ tự câu hỏi " + request.getQuestionOrder() + " đã được sử dụng bởi câu hỏi khác");
        }

        boolean hasCorrect = request.getAnswers().stream().anyMatch(QuizAnswerRequest::isCorrect);
        if (!hasCorrect) {
            throw new BadRequestException("Câu hỏi phải có ít nhất 1 đáp án đúng");
        }

        question.setQuestionOrder(request.getQuestionOrder());
        question.setQuestionText(request.getQuestionText());
        quizQuestionRepository.save(question);

        quizAnswerRepository.deleteAllByQuestionId(question.getId());
        for (QuizAnswerRequest aReq : request.getAnswers()) {
            QuizAnswer answer = new QuizAnswer();
            answer.setQuestion(question);
            answer.setAnswerText(aReq.getAnswerText());
            answer.setCorrect(aReq.isCorrect());
            quizAnswerRepository.save(answer);
        }

        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        return mapToQuizResponse(quiz);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID quizId, UUID questionId) {
        User currentUser = getCurrentUser();
        assertOwnership(quizId, currentUser);

        QuizQuestion question = quizQuestionRepository.findByIdAndQuizIdAndIsDeleteFalse(questionId, quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong quiz này"));

        quizAnswerRepository.deleteAllByQuestionId(question.getId());
        question.setDelete(true);
        quizQuestionRepository.save(question);
    }


    @Override
    @Transactional
    public ImportResultResponse importQuizFromExcel(MultipartFile file) {
        User currentUser = getCurrentUser();

        School school = null;
        Partnership partnership = null;
        if (currentUser.getRole() == Role.PARTNERSHIP_SCHOOL) {
            school = resolveSchool(currentUser.getId());
        } else {
            partnership = resolvePartnership(currentUser.getId());
        }

        List<QuizExcelRowDto> rows = parseExcelRows(file);
        LinkedHashMap<String, List<QuizExcelRowDto>> grouped = groupByQuizTitle(rows);

        List<ImportErrorDetail> errors = new ArrayList<>();
        int successCount = 0;

        for (Map.Entry<String, List<QuizExcelRowDto>> entry : grouped.entrySet()) {
            String quizTitle = entry.getKey();
            List<QuizExcelRowDto> quizRows = entry.getValue();

            List<ImportErrorDetail> groupErrors = validateQuizGroup(quizTitle, quizRows);
            if (!groupErrors.isEmpty()) {
                errors.addAll(groupErrors);
                continue;
            }

            try {
                QuizExcelRowDto firstRow = quizRows.get(0);
                Quiz quiz = buildAndSaveQuiz(
                        quizTitle,
                        firstRow.getDescription(),
                        QuizDifficulty.valueOf(firstRow.getDifficulty().toUpperCase()),
                        QuizType.valueOf(firstRow.getQuizType().toUpperCase()),
                        parseIntOrNull(firstRow.getTargetGrade()),
                        parseIntOrDefault(firstRow.getCoinsOnPass(), 10),
                        parseIntOrNull(firstRow.getTimePerQuestion()),
                        parseIntOrDefault(firstRow.getPassScorePercentage(), 80),
                        QuizCreated.IMPORT, school, partnership
                );

                for (QuizExcelRowDto row : quizRows) {
                    QuizQuestion question = new QuizQuestion();
                    question.setQuiz(quiz);
                    question.setQuestionOrder(Integer.parseInt(row.getQuestionOrder()));
                    question.setQuestionText(row.getQuestionText());
                    question = quizQuestionRepository.save(question);

                    saveAnswersFromExcelRow(question, row);
                }

                successCount++;
            } catch (Exception e) {
                errors.add(ExcelUtil.buildError(
                        quizRows.get(0).getRowNumber(), "general",
                        "Lỗi tạo quiz '" + quizTitle + "': " + e.getMessage()));
            }
        }

        return ImportResultResponse.builder()
                .totalRows(grouped.size())
                .successCount(successCount)
                .failCount(errors.stream()
                        .map(ImportErrorDetail::getRowNumber)
                        .collect(Collectors.toSet()).size())
                .errors(errors)
                .build();
    }

    private List<QuizExcelRowDto> parseExcelRows(MultipartFile file) {
        ExcelUtil.validateFile(file);
        List<QuizExcelRowDto> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new BadRequestException("File Excel không có sheet nào");

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BadRequestException("File Excel không có header row");

            ExcelUtil.validateHeaders(headerRow, EXCEL_HEADERS);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || ExcelUtil.isRowEmpty(row, EXCEL_HEADERS.length)) continue;

                QuizExcelRowDto dto = QuizExcelRowDto.builder()
                        .rowNumber(i + 1)
                        .quizTitle(ExcelUtil.getCellStringValue(row.getCell(0)))
                        .description(ExcelUtil.getCellStringValue(row.getCell(1)))
                        .difficulty(ExcelUtil.getCellStringValue(row.getCell(2)))
                        .targetGrade(ExcelUtil.getCellStringValue(row.getCell(3)))
                        .quizType(ExcelUtil.getCellStringValue(row.getCell(4)))
                        .questionOrder(ExcelUtil.getCellStringValue(row.getCell(5)))
                        .questionText(ExcelUtil.getCellStringValue(row.getCell(6)))
                        .answerA(ExcelUtil.getCellStringValue(row.getCell(7)))
                        .answerB(ExcelUtil.getCellStringValue(row.getCell(8)))
                        .answerC(ExcelUtil.getCellStringValue(row.getCell(9)))
                        .answerD(ExcelUtil.getCellStringValue(row.getCell(10)))
                        .correctAnswer(ExcelUtil.getCellStringValue(row.getCell(11)))
                        .coinsOnPass(ExcelUtil.getCellStringValue(row.getCell(12)))
                        .timePerQuestion(ExcelUtil.getCellStringValue(row.getCell(13)))
                        .passScorePercentage(ExcelUtil.getCellStringValue(row.getCell(14)))
                        .build();
                rows.add(dto);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi đọc file Excel: " + e.getMessage());
        }

        if (rows.isEmpty()) throw new BadRequestException("File Excel không có dữ liệu");
        return rows;
    }

    private LinkedHashMap<String, List<QuizExcelRowDto>> groupByQuizTitle(List<QuizExcelRowDto> rows) {
        LinkedHashMap<String, List<QuizExcelRowDto>> grouped = new LinkedHashMap<>();
        for (QuizExcelRowDto row : rows) {
            grouped.computeIfAbsent(row.getQuizTitle(), k -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private List<ImportErrorDetail> validateQuizGroup(String quizTitle, List<QuizExcelRowDto> rows) {
        List<ImportErrorDetail> errors = new ArrayList<>();
        int firstRow = rows.get(0).getRowNumber();

        if (ExcelUtil.isBlank(quizTitle)) {
            errors.add(ExcelUtil.buildError(firstRow, "quiz_title", "Tiêu đề quiz không được rỗng"));
            return errors;
        }

        QuizExcelRowDto firstRowDto = rows.get(0);
        if (!isValidEnum(QuizDifficulty.class, firstRowDto.getDifficulty())) {
            errors.add(ExcelUtil.buildError(firstRow, "difficulty",
                    "Độ khó không hợp lệ: " + firstRowDto.getDifficulty() + ". Phải là EASY, MEDIUM hoặc HARD"));
        }
        if (!ExcelUtil.isBlank(firstRowDto.getTargetGrade())) {
            try {
                int grade = Integer.parseInt(firstRowDto.getTargetGrade().trim());
                if (grade < 1 || grade > 12) {
                    errors.add(ExcelUtil.buildError(firstRow, "target_grade", "Khối lớp phải từ 1 đến 12"));
                }
            } catch (NumberFormatException e) {
                errors.add(ExcelUtil.buildError(firstRow, "target_grade", "Khối lớp phải là số nguyên"));
            }
        }
        if (!isValidEnum(QuizType.class, firstRowDto.getQuizType())) {
            errors.add(ExcelUtil.buildError(firstRow, "quiz_type",
                    "Loại quiz không hợp lệ: " + firstRowDto.getQuizType()));
        }

        Set<Integer> usedOrders = new HashSet<>();
        for (QuizExcelRowDto row : rows) {
            if (ExcelUtil.isBlank(row.getQuestionText())) {
                errors.add(ExcelUtil.buildError(row.getRowNumber(), "question_text", "Nội dung câu hỏi không được rỗng"));
            }

            if (ExcelUtil.isBlank(row.getQuestionOrder())) {
                errors.add(ExcelUtil.buildError(row.getRowNumber(), "question_order", "Thứ tự câu hỏi không được rỗng"));
            } else {
                try {
                    int order = Integer.parseInt(row.getQuestionOrder());
                    if (!usedOrders.add(order)) {
                        errors.add(ExcelUtil.buildError(row.getRowNumber(), "question_order",
                                "Thứ tự câu hỏi " + order + " bị trùng trong cùng quiz"));
                    }
                } catch (NumberFormatException e) {
                    errors.add(ExcelUtil.buildError(row.getRowNumber(), "question_order", "Thứ tự câu hỏi phải là số nguyên"));
                }
            }

            if (ExcelUtil.isBlank(row.getAnswerA()) || ExcelUtil.isBlank(row.getAnswerB())) {
                errors.add(ExcelUtil.buildError(row.getRowNumber(), "answer_A/B", "Phải có ít nhất 2 đáp án (A và B)"));
            }

            String correct = row.getCorrectAnswer().toUpperCase().trim();
            if (!correct.matches("[ABCD]")) {
                errors.add(ExcelUtil.buildError(row.getRowNumber(), "correct_answer",
                        "Đáp án đúng phải là A, B, C hoặc D"));
            } else {
                if (correct.equals("C") && ExcelUtil.isBlank(row.getAnswerC())) {
                    errors.add(ExcelUtil.buildError(row.getRowNumber(), "answer_C",
                            "Đáp án đúng là C nhưng cột answer_C trống"));
                }
                if (correct.equals("D") && ExcelUtil.isBlank(row.getAnswerD())) {
                    errors.add(ExcelUtil.buildError(row.getRowNumber(), "answer_D",
                            "Đáp án đúng là D nhưng cột answer_D trống"));
                }
            }
        }

        return errors;
    }

    private void saveAnswersFromExcelRow(QuizQuestion question, QuizExcelRowDto row) {
        String correct = row.getCorrectAnswer().toUpperCase().trim();
        String[][] answerPairs = {
                {"A", row.getAnswerA()},
                {"B", row.getAnswerB()},
                {"C", row.getAnswerC()},
                {"D", row.getAnswerD()}
        };

        for (String[] pair : answerPairs) {
            String label = pair[0];
            String text = pair[1];
            if (!ExcelUtil.isBlank(text)) {
                QuizAnswer answer = new QuizAnswer();
                answer.setQuestion(question);
                answer.setAnswerText(text);
                answer.setCorrect(label.equals(correct));
                quizAnswerRepository.save(answer);
            }
        }
    }

    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        if (ExcelUtil.isBlank(value)) return false;
        try {
            Enum.valueOf(enumClass, value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Integer parseIntOrDefault(String value, int defaultValue) {
        if (ExcelUtil.isBlank(value)) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseIntOrNull(String value) {
        if (ExcelUtil.isBlank(value)) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
