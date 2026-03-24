package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.DefaultCoinConfig;
import com.sep490.ecoverse_be.enums.QuizDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DefaultCoinConfigRepository extends JpaRepository<DefaultCoinConfig, UUID> {

    Optional<DefaultCoinConfig> findByGameTypeIdAndDifficulty(UUID gameTypeId, QuizDifficulty difficulty);
}

