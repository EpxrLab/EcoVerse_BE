package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnershipRepository extends JpaRepository<Partnership, UUID> {

    List<Partnership> findByApprovalStatus(ApprovalStatus approvalStatus);
}
