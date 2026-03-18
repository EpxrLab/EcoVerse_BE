package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByCreatedByIdAndIsActiveTrueOrderByCreatedAtDesc(UUID createdById);

    Optional<Quiz> findByIdAndCreatedByIdAndIsActiveTrue(UUID id, UUID createdById);
}
