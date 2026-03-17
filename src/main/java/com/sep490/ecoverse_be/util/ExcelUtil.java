package com.sep490.ecoverse_be.util;

import com.sep490.ecoverse_be.dto.response.ImportErrorDetail;
import com.sep490.ecoverse_be.exception.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

public class ExcelUtil {

    private ExcelUtil() {}

    public static void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File không được rỗng");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BadRequestException("Chỉ hỗ trợ file Excel (.xlsx, .xls)");
        }
    }

    public static void validateHeaders(Row headerRow, String[] expected) {
        for (int i = 0; i < expected.length; i++) {
            Cell cell = headerRow.getCell(i);
            String value = (cell != null) ? cell.getStringCellValue().trim() : "";
            if (!expected[i].equalsIgnoreCase(value)) {
                throw new BadRequestException(
                        "Header cột " + (i + 1) + " phải là '" + expected[i] + "', nhận được: '" + value + "'");
            }
        }
    }

    public static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    public static boolean isRowEmpty(Row row, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellStringValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static ImportErrorDetail buildError(int rowNumber, String field, String message) {
        return ImportErrorDetail.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }
}
