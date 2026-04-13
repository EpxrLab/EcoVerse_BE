package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID>,
        JpaSpecificationExecutor<School> {

    List<School> findByApprovalStatus(ApprovalStatus approvalStatus);

    Optional<School> findByUserId(UUID userId);

    List<School> findByWardAndApprovalStatus(String ward, ApprovalStatus approvalStatus);

    List<School> findByProvinceAndApprovalStatus(String province, ApprovalStatus approvalStatus);

    long countByApprovalStatus(ApprovalStatus approvalStatus);
}
