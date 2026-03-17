package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnershipRepository extends JpaRepository<Partnership, UUID>,
        JpaSpecificationExecutor<Partnership> {

    List<Partnership> findByApprovalStatus(ApprovalStatus approvalStatus);

    Optional<Partnership> findByUserId(UUID userId);
}
