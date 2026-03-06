package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.StudentExcelRowDto;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.Gender;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IEmailService;
import com.sep490.ecoverse_be.service.IStudentImportService;
import com.sep490.ecoverse_be.util.PasswordGenerator;
import com.sep490.ecoverse_be.util.StudentCodeGenerator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class StudentImportServiceImpl implements IStudentImportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private IEmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] EXPECTED_HEADERS = {
            "Student Full Name", "Class Name", "Grade Level", "Date of Birth",
            "Gender", "Address", "Parent Full Name", "Parent Phone Number", "Parent Email"
    };

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    @Override
    @Transactional
    public ImportResultResponse importStudentsFromExcel(MultipartFile file) {
        UUID schoolId = getCurrentUserId();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        List<StudentExcelRowDto> rows = parseExcel(file);
        List<ImportErrorDetail> errors = new ArrayList<>();
        int successCount = 0;

        Set<String> existingCodes = new HashSet<>(
                studentRepository.findBySchoolId(schoolId).stream()
                        .map(Student::getStudentCode)
                        .toList()
        );

        Map<String, String> parentPasswordMap = new HashMap<>();
        Map<String, String> studentPasswordMap = new HashMap<>();

        for (StudentExcelRowDto row : rows) {
            List<ImportErrorDetail> rowErrors = validateRow(row);
            if (!rowErrors.isEmpty()) {
                errors.addAll(rowErrors);
                continue;
            }

            try {
                String studentCode = StudentCodeGenerator.generateUniqueCode(row.getStudentFullName(), existingCodes);
                existingCodes.add(studentCode);

                String studentRawPassword = PasswordGenerator.generate();
                String parentRawPassword;

                Parent parent;
                Optional<Parent> existingParent = parentRepository.findByPhoneNumber(row.getParentPhone());

                if (existingParent.isPresent()) {
                    parent = existingParent.get();
                    parentRawPassword = parentPasswordMap.getOrDefault(row.getParentPhone(), "(đã tạo trước đó)");
                } else {
                    parentRawPassword = PasswordGenerator.generate();

                    User parentUser = new User();
                    parentUser.setEmail(row.getParentEmail());
                    parentUser.setUsername(row.getParentPhone());
                    parentUser.setPasswordHash(passwordEncoder.encode(parentRawPassword));
                    parentUser.setRole(Role.PARENT);
                    parentUser.setStatus(AccountStatus.ACTIVE);
                    parentUser.setIsActive(true);
                    parentUser = userRepository.save(parentUser);

                    parent = new Parent();
                    parent.setUser(parentUser);
                    parent.setFullName(row.getParentFullName());
                    parent.setPhoneNumber(row.getParentPhone());
                    parent = parentRepository.save(parent);

                    parentPasswordMap.put(row.getParentPhone(), parentRawPassword);
                }

                User studentUser = new User();
                studentUser.setEmail(null);
                studentUser.setUsername(studentCode);
                studentUser.setPasswordHash(passwordEncoder.encode(studentRawPassword));
                studentUser.setRole(Role.STUDENT);
                studentUser.setStatus(AccountStatus.ACTIVE);
                studentUser.setIsActive(true);
                studentUser = userRepository.save(studentUser);

                Student student = new Student();
                student.setUser(studentUser);
                student.setSchool(school);
                student.setStudentCode(studentCode);
                student.setFullName(row.getStudentFullName());
                student.setClassName(row.getClassName());
                student.setGradeLevel(row.getGradeLevel());
                student.setAvatarUrl("https://res.cloudinary.com/dsqlivxid/image/upload/v1772641395/ecoverse/user/8f1ca2029e2efceebd22fa05cca423d7.jpg_20260304232313.jpg");
                student.setDateOfBirth(LocalDate.parse(row.getDateOfBirth(), DATE_FORMAT));
                student.setGender(Gender.valueOf(row.getGender().toUpperCase()));
                studentRepository.save(student);

                studentPasswordMap.put(studentCode, studentRawPassword);

                if (!studentParentLinkRepository.existsByStudentIdAndParentId(student.getId(), parent.getId())) {
                    StudentParentLink link = new StudentParentLink();
                    link.setStudent(student);
                    link.setParent(parent);
                    studentParentLinkRepository.save(link);
                }

                successCount++;
            } catch (Exception e) {
                errors.add(ImportErrorDetail.builder()
                        .rowNumber(row.getRowNumber())
                        .field("general")
                        .message("Lỗi xử lý dòng: " + e.getMessage())
                        .build());
            }
        }

        return ImportResultResponse.builder()
                .totalRows(rows.size())
                .successCount(successCount)
                .failCount(errors.size())
                .errors(errors)
                .build();
    }

    @Override
    public AccountListResponse getImportedAccounts() {
        UUID schoolId = getCurrentUserId();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        List<Student> students = studentRepository.findBySchoolId(schoolId);

        Map<UUID, List<Student>> parentStudentMap = new LinkedHashMap<>();
        Map<UUID, Parent> parentMap = new HashMap<>();

        for (Student student : students) {
            List<StudentParentLink> links = studentParentLinkRepository.findByStudentId(student.getId());
            for (StudentParentLink link : links) {
                Parent parent = link.getParent();
                parentMap.putIfAbsent(parent.getId(), parent);
                parentStudentMap.computeIfAbsent(parent.getId(), k -> new ArrayList<>()).add(student);
            }
        }

        List<ParentAccountInfo> accounts = parentStudentMap.entrySet().stream()
                .map(entry -> {
                    Parent parent = parentMap.get(entry.getKey());
                    List<StudentAccountInfo> children = entry.getValue().stream()
                            .map(s -> StudentAccountInfo.builder()
                                    .studentFullName(s.getFullName())
                                    .studentCode(s.getStudentCode())
                                    .className(s.getClassName())
                                    .gradeLevel(s.getGradeLevel())
                                    .build())
                            .toList();

                    return ParentAccountInfo.builder()
                            .parentFullName(parent.getFullName())
                            .phoneNumber(parent.getPhoneNumber())
                            .parentEmail(parent.getUser().getEmail())
                            .children(children)
                            .build();
                })
                .toList();

        return AccountListResponse.builder()
                .schoolName(school.getSchoolName())
                .totalStudents(students.size())
                .totalParents(parentMap.size())
                .accounts(accounts)
                .build();
    }

    @Override
    @Transactional
    public void sendCredentialEmails() {
        UUID schoolId = getCurrentUserId();
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        List<Student> students = studentRepository.findBySchoolId(schoolId);

        Map<UUID, List<Student>> parentStudentMap = new LinkedHashMap<>();
        Map<UUID, Parent> parentMap = new HashMap<>();

        for (Student student : students) {
            List<StudentParentLink> links = studentParentLinkRepository.findByStudentId(student.getId());
            for (StudentParentLink link : links) {
                Parent parent = link.getParent();
                parentMap.putIfAbsent(parent.getId(), parent);
                parentStudentMap.computeIfAbsent(parent.getId(), k -> new ArrayList<>()).add(student);
            }
        }

        for (Map.Entry<UUID, List<Student>> entry : parentStudentMap.entrySet()) {
            Parent parent = parentMap.get(entry.getKey());

            String parentRawPassword = PasswordGenerator.generate();
            parent.getUser().setPasswordHash(passwordEncoder.encode(parentRawPassword));
            userRepository.save(parent.getUser());

            List<StudentAccountInfo> children = new ArrayList<>();
            for (Student student : entry.getValue()) {
                String studentRawPassword = PasswordGenerator.generate();
                student.getUser().setPasswordHash(passwordEncoder.encode(studentRawPassword));
                userRepository.save(student.getUser());

                children.add(StudentAccountInfo.builder()
                        .studentFullName(student.getFullName())
                        .studentCode(student.getStudentCode())
                        .password(studentRawPassword)
                        .className(student.getClassName())
                        .gradeLevel(student.getGradeLevel())
                        .build());
            }

            String parentEmail = parent.getUser().getEmail();
            if (parentEmail != null && !parentEmail.isBlank()) {
                emailService.sendCredentialEmail(
                        parentEmail,
                        parent.getFullName(),
                        parent.getPhoneNumber(),
                        parentRawPassword,
                        children
                );
            }
        }
    }


    private List<StudentExcelRowDto> parseExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File không được rỗng");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BadRequestException("Chỉ hỗ trợ file Excel (.xlsx, .xls)");
        }

        List<StudentExcelRowDto> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("File Excel không có sheet nào");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("File Excel không có header row");
            }

            validateHeaders(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                StudentExcelRowDto dto = new StudentExcelRowDto();
                dto.setRowNumber(i + 1);
                dto.setStudentFullName(getCellStringValue(row.getCell(0)));
                dto.setClassName(getCellStringValue(row.getCell(1)));
                dto.setGradeLevel(getCellStringValue(row.getCell(2)));
                dto.setDateOfBirth(getCellStringValue(row.getCell(3)));
                dto.setGender(getCellStringValue(row.getCell(4)));
                dto.setAddress((getCellStringValue(row.getCell(5))));
                dto.setParentFullName(getCellStringValue(row.getCell(6)));
                dto.setParentPhone(getCellStringValue(row.getCell(7)));
                dto.setParentEmail(getCellStringValue(row.getCell(8)));
                rows.add(dto);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi đọc file Excel: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("File Excel không có dữ liệu");
        }

        return rows;
    }

    private void validateHeaders(Row headerRow) {
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            Cell cell = headerRow.getCell(i);
            String value = (cell != null) ? cell.getStringCellValue().trim() : "";
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(value)) {
                throw new BadRequestException(
                        "Header cột " + (i + 1) + " phải là '" + EXPECTED_HEADERS[i] + "', nhận được: '" + value + "'");
            }
        }
    }

    private List<ImportErrorDetail> validateRow(StudentExcelRowDto row) {
        List<ImportErrorDetail> errors = new ArrayList<>();

        if (isBlank(row.getStudentFullName())) {
            errors.add(buildError(row.getRowNumber(), "Student Full Name", "Tên học sinh không được rỗng"));
        }

        if (isBlank(row.getClassName())) {
            errors.add(buildError(row.getRowNumber(), "Class Name", "Tên lớp không được rỗng"));
        }

        if (isBlank(row.getGradeLevel())) {
            errors.add(buildError(row.getRowNumber(), "Grade Level", "Khối lớp không được rỗng"));
        }

        if (isBlank(row.getDateOfBirth())) {
            errors.add(buildError(row.getRowNumber(), "Date of Birth", "Ngày sinh không được rỗng"));
        } else {
            try {
                LocalDate.parse(row.getDateOfBirth(), DATE_FORMAT);
            } catch (DateTimeParseException e) {
                errors.add(buildError(row.getRowNumber(), "Date of Birth", "Ngày sinh không đúng định dạng YYYY-MM-DD"));
            }
        }

        if (isBlank(row.getGender())) {
            errors.add(buildError(row.getRowNumber(), "Gender", "Giới tính không được rỗng"));
        } else if (!row.getGender().equalsIgnoreCase("MALE") && !row.getGender().equalsIgnoreCase("FEMALE")) {
            errors.add(buildError(row.getRowNumber(), "Gender", "Giới tính phải là Male hoặc Female"));
        }

        if(isBlank(row.getAddress())){
            errors.add(buildError(row.getRowNumber(), "Address", "Địa chỉ không được rỗng"));
        }

        if (isBlank(row.getParentFullName())) {
            errors.add(buildError(row.getRowNumber(), "Parent Full Name", "Tên phụ huynh không được rỗng"));
        }

        if (isBlank(row.getParentPhone())) {
            errors.add(buildError(row.getRowNumber(), "Parent Phone Number", "Số điện thoại phụ huynh không được rỗng"));
        } else if (!row.getParentPhone().matches("^(03|05|07|08|09)[0-9]{8}$")) {
            errors.add(buildError(row.getRowNumber(), "Parent Phone Number", "Số điện thoại phải có 10 chữ số, bắt đầu bằng 0"));
        }

        if (isBlank(row.getParentEmail())) {
            errors.add(buildError(row.getRowNumber(), "Parent Email", "Email phụ huynh không được rỗng"));
        } else if (!row.getParentEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            errors.add(buildError(row.getRowNumber(), "Parent Email", "Email không hợp lệ"));
        }

        return errors;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMAT);
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue().trim();
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !getCellStringValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ImportErrorDetail buildError(int rowNumber, String field, String message) {
        return ImportErrorDetail.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }
}
