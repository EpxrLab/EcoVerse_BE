package com.sep490.ecoverse_be.util;

import com.sep490.ecoverse_be.exception.FuncErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUpLoadUtil {
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    // Giới hạn riêng cho 3D model (250MB)
    public static final long MAX_MODEL_SIZE = 250L * 1024 * 1024;

    public static final String IMAGE_PATTERN = "(.+\\.(?i)(jpg|png|gif|bmp))$";

    // Pattern dành riêng cho 3D model .glb
    public static final String MODEL_PATTERN = "(.+\\.(?i)(glb|gltf))$";

    public static final String VIDEO_PATTERN = "(.+\\.(?i)(mp4|mov|avi|mkv))$";

    public static final String DATE_FORMAT = "yyyyMMddHHmmss";

    public static final String FILE_NAME_FORMAT = "%s_%s";

    public static boolean isAllowedExtension(final String fileName, final String pattern) {
        final Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(fileName);
        return matcher.matches();
    }

    public static void assertAllowed(MultipartFile file, String pattern) {
        final long size = file.getSize();
        if (size > MAX_FILE_SIZE) {
            throw new FuncErrorException("Kích thước file tối đa là 100MB");
        }
        if (!isAllowedExtension(file.getOriginalFilename(), pattern)) {
            throw new FuncErrorException("Chỉ hỗ trợ các định dạng: " + pattern);
        }
    }

    // Validate riêng cho 3D model, cho phép tới 250MB
    public static void assertModelAllowed(MultipartFile file) {
        final long size = file.getSize();
        if (size > MAX_MODEL_SIZE) {
            throw new FuncErrorException("Kích thước file 3D model tối đa là 250MB");
        }
        if (!isAllowedExtension(file.getOriginalFilename(), MODEL_PATTERN)) {
            throw new FuncErrorException("Chỉ hỗ trợ định dạng 3D model: .glb, .gltf");
        }
    }

    public static String getFileName(final String name) {
        final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final String date = dateFormat.format(System.currentTimeMillis());
        return String.format(FILE_NAME_FORMAT, name, date);
    }
}
