package com.sep490.ecoverse_be.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cấu hình Jackson để serialize/deserialize LocalDateTime theo chuẩn UTC (có hậu tố "Z").
 * <p>
 * Frontend gửi timestamp dạng "2026-04-13T16:00:00.000Z" (UTC).
 * Nếu không cấu hình, backend response trả về "2026-04-13T16:00:00" (thiếu "Z"),
 * khiến frontend hiểu là local time → bị lệch 7 tiếng (UTC+7).
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Bean
    public SimpleModule localDateTimeModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(UTC_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(UTC_FORMATTER));
        return module;
    }
}
