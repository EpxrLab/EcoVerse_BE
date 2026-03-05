package com.sep490.ecoverse_be.util;

import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

public final class StudentCodeGenerator {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private StudentCodeGenerator() {}


    public static String generateBaseCode(String fullName) {
        String normalized = removeDiacritics(fullName.trim());

        String[] parts = normalized.split("\\s+");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Full name must not be empty");
        }

        if (parts.length == 1) {
            return capitalize(parts[0]);
        }

        StringBuilder code = new StringBuilder();
        code.append(capitalize(parts[parts.length - 1]));

        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                code.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }

        return code.toString();
    }

    public static String generateUniqueCode(String fullName, Set<String> existingCodes) {
        String baseCode = generateBaseCode(fullName);

        if (!existingCodes.contains(baseCode)) {
            return baseCode;
        }

        int suffix = 1;
        while (existingCodes.contains(baseCode + suffix)) {
            suffix++;
        }
        return baseCode + suffix;
    }

    public static String removeDiacritics(String input) {
        String result = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = Normalizer.normalize(result, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }

    private static String capitalize(String word) {
        if (word == null || word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
}
