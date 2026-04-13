package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByIdAndIsActiveTrue(UUID id);

    // School-owned quizzes (chỉ quiz đang active — DB không có cột is_delete)
    List<Quiz> findBySchoolIdAndIsActiveTrueOrderByCreatedAtDesc(UUID schoolId);

    Optional<Quiz> findByIdAndSchoolIdAndIsActiveTrue(UUID id, UUID schoolId);

    List<Quiz> findByIdInAndSchoolIdAndIsActiveTrue(List<UUID> ids, UUID schoolId);

    // Partnership-owned quizzes
    List<Quiz> findByPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(UUID partnershipId);

    Optional<Quiz> findByIdAndPartnershipIdAndIsActiveTrue(UUID id, UUID partnershipId);

    List<Quiz> findByIdInAndPartnershipIdAndIsActiveTrue(List<UUID> ids, UUID partnershipId);
}
