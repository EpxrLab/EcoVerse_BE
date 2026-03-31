package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByIdAndIsDeleteFalse(UUID id);

    // School-owned quizzes
    List<Quiz> findBySchoolIdAndIsDeleteFalseOrderByCreatedAtDesc(UUID schoolId);

    Optional<Quiz> findByIdAndSchoolIdAndIsDeleteFalse(UUID id, UUID schoolId);

    List<Quiz> findByIdInAndSchoolIdAndIsDeleteFalse(List<UUID> ids, UUID schoolId);

    // Partnership-owned quizzes
    List<Quiz> findByPartnershipIdAndIsDeleteFalseOrderByCreatedAtDesc(UUID partnershipId);

    Optional<Quiz> findByIdAndPartnershipIdAndIsDeleteFalse(UUID id, UUID partnershipId);

    List<Quiz> findByIdInAndPartnershipIdAndIsDeleteFalse(List<UUID> ids, UUID partnershipId);
}
