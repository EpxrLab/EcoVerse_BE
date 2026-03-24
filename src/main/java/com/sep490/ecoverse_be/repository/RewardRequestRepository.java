package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.RewardRequest;
import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardRequestRepository extends JpaRepository<RewardRequest, UUID> {

    List<RewardRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<RewardRequest> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    List<RewardRequest> findBySchoolIdAndStatusOrderByCreatedAtDesc(UUID schoolId, RewardRequestStatus status);

    Optional<RewardRequest> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<RewardRequest> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByStudentIdAndRewardIdAndStatusIn(UUID studentId, UUID rewardId, List<RewardRequestStatus> statuses);
}
