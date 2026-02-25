package com.sep490.ecoverse_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class EcoVerseBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoVerseBeApplication.class, args);
    }

}
