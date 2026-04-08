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

    private static final String[] QUESTION_EXCEL_HEADERS = {
            "quiz_type", "question_text",
            "answer_A", "answer_B", "answer_C", "answer_D",
            "correct_answer"
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
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdAndIsActiveTrueOrderByQuestionOrder(quiz.getId());
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
        quiz.setCoinsOnPass(coinOnPass != null ? coinOnPass : 0);
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

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            saveQuestionsAndAnswers(quiz, request.getQuestions());
        }

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
                .map(q -> mapToSummary(q, quizQuestionRepository.countByQuizIdAndIsActiveTrue(q.getId())))
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
                .findByQuizIdAndIsActiveTrueOrderByQuestionOrder(quiz.getId());
        for (QuizQuestion question : activeQuestions) {
            question.setActive(false);
            quizQuestionRepository.save(question);
        }
    }

    @Override
    @Transactional
    public QuizResponse togglePublish(UUID quizId) {
        User currentUser = getCurrentUser();
        Quiz quiz = assertOwnership(quizId, currentUser);

        if (!quiz.isPublished()) {
            int questionCount = quizQuestionRepository.countByQuizIdAndIsActiveTrue(quiz.getId());
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
                .findByQuizIdAndIsActiveTrueOrderByQuestionOrder(quiz.getId())
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

        QuizQuestion question = quizQuestionRepository.findByIdAndQuizIdAndIsActiveTrue(questionId, quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong quiz này"));

        boolean orderChanged = question.getQuestionOrder() != request.getQuestionOrder();
        if (orderChanged && quizQuestionRepository.existsByQuizIdAndQuestionOrderAndIsActiveTrue(quizId, request.getQuestionOrder())) {
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

        QuizQuestion question = quizQuestionRepository.findByIdAndQuizIdAndIsActiveTrue(questionId, quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong quiz này"));

        quizAnswerRepository.deleteAllByQuestionId(question.getId());
        question.setActive(false);
        quizQuestionRepository.save(question);
    }


    /**
     * Parse file Excel và trả về danh sách câu hỏi dưới dạng QuizQuestionRequest.
     * Không ghi DB — chỉ dùng để UI hiển thị preview trước khi user xác nhận tạo quiz.
     * Excel format: quiz_type | question_text | answer_A | answer_B | answer_C | answer_D | correct_answer
     * questionOrder được tự động gán theo vị trí dòng trong file (1-based).
     */
    @Override
    public List<QuizQuestionRequest> previewQuestionsFromExcel(MultipartFile file) {
        List<QuizExcelRowDto> rows = parseQuestionExcelRows(file);
        List<String> errors = validatePreviewRows(rows);

        if (!errors.isEmpty()) {
            throw new BadRequestException("File Excel có lỗi:\n" + String.join("\n", errors));
        }

        List<QuizQuestionRequest> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            result.add(convertRowToQuestionRequest(rows.get(i), i + 1));
        }
        return result;
    }

    private List<QuizExcelRowDto> parseQuestionExcelRows(MultipartFile file) {
        ExcelUtil.validateFile(file);
        List<QuizExcelRowDto> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new BadRequestException("File Excel không có sheet nào");

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BadRequestException("File Excel không có header row");

            ExcelUtil.validateHeaders(headerRow, QUESTION_EXCEL_HEADERS);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || ExcelUtil.isRowEmpty(row, QUESTION_EXCEL_HEADERS.length)) continue;

                QuizExcelRowDto dto = QuizExcelRowDto.builder()
                        .rowNumber(i + 1)
                        .quizType(ExcelUtil.getCellStringValue(row.getCell(0)))
                        .questionText(ExcelUtil.getCellStringValue(row.getCell(1)))
                        .answerA(ExcelUtil.getCellStringValue(row.getCell(2)))
                        .answerB(ExcelUtil.getCellStringValue(row.getCell(3)))
                        .answerC(ExcelUtil.getCellStringValue(row.getCell(4)))
                        .answerD(ExcelUtil.getCellStringValue(row.getCell(5)))
                        .correctAnswer(ExcelUtil.getCellStringValue(row.getCell(6)))
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

    private static final Set<String> VALID_QUIZ_TYPES = Arrays.stream(QuizType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private List<String> validatePreviewRows(List<QuizExcelRowDto> rows) {
        List<String> errors = new ArrayList<>();

        for (QuizExcelRowDto row : rows) {
            String prefix = "Dòng " + row.getRowNumber() + ": ";

            if (ExcelUtil.isBlank(row.getQuizType())) {
                errors.add(prefix + "quiz_type không được rỗng");
            } else if (!VALID_QUIZ_TYPES.contains(row.getQuizType().toUpperCase().trim())) {
                errors.add(prefix + "quiz_type không hợp lệ (hợp lệ: MULTIPLE_CHOICE, TRUE_FALSE, DRAG_DROP)");
            }

            if (ExcelUtil.isBlank(row.getQuestionText())) {
                errors.add(prefix + "nội dung câu hỏi không được rỗng");
            }

            if (ExcelUtil.isBlank(row.getAnswerA()) || ExcelUtil.isBlank(row.getAnswerB())) {
                errors.add(prefix + "phải có ít nhất 2 đáp án (A và B)");
            }

            if (ExcelUtil.isBlank(row.getCorrectAnswer()) || !row.getCorrectAnswer().toUpperCase().trim().matches("[ABCD]")) {
                errors.add(prefix + "đáp án đúng phải là A, B, C hoặc D");
            } else {
                String correct = row.getCorrectAnswer().toUpperCase().trim();
                if (correct.equals("C") && ExcelUtil.isBlank(row.getAnswerC())) {
                    errors.add(prefix + "đáp án đúng là C nhưng cột answer_C trống");
                }
                if (correct.equals("D") && ExcelUtil.isBlank(row.getAnswerD())) {
                    errors.add(prefix + "đáp án đúng là D nhưng cột answer_D trống");
                }
            }
        }
        return errors;
    }

    private QuizQuestionRequest convertRowToQuestionRequest(QuizExcelRowDto row, int rowIndex) {
        String correct = row.getCorrectAnswer().toUpperCase().trim();

        List<QuizAnswerRequest> answers = new ArrayList<>();
        String[][] pairs = {
                {"A", row.getAnswerA()},
                {"B", row.getAnswerB()},
                {"C", row.getAnswerC()},
                {"D", row.getAnswerD()}
        };
        for (String[] pair : pairs) {
            if (!ExcelUtil.isBlank(pair[1])) {
                answers.add(QuizAnswerRequest.builder()
                        .answerText(pair[1])
                        .correct(pair[0].equals(correct))
                        .build());
            }
        }

        return QuizQuestionRequest.builder()
                .questionOrder(rowIndex)
                .questionText(row.getQuestionText())
                .answers(answers)
                .build();
    }

}
