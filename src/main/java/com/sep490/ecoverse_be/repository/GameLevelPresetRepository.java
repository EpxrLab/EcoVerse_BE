package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.GameLevelPreset;
import com.sep490.ecoverse_be.enums.QuizDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameLevelPresetRepository extends JpaRepository<GameLevelPreset, UUID> {

    List<GameLevelPreset> findByGameTypeIdOrderByDifficultyAsc(UUID gameTypeId);

    List<GameLevelPreset> findByIdInAndGameTypeId(List<UUID> ids, UUID gameTypeId);

    Optional<GameLevelPreset> findByGameTypeIdAndDifficulty(UUID gameTypeId, QuizDifficulty difficulty);

    boolean existsByGameTypeIdAndDifficulty(UUID gameTypeId, QuizDifficulty difficulty);

    boolean existsByGameTypeIdAndDifficultyAndIdNot(UUID gameTypeId, QuizDifficulty difficulty, UUID id);
}

