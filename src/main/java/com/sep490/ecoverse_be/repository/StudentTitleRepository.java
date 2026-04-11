package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.StudentTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentTitleRepository extends JpaRepository<StudentTitle, UUID> {

    boolean existsByStudentIdAndCampaignTitleId(UUID studentId, UUID campaignTitleId);

    List<StudentTitle> findByStudentIdOrderByEarnedAtDesc(UUID studentId);

    @Query("SELECT st FROM StudentTitle st WHERE st.campaign.id = :campaignId ORDER BY st.earnedAt DESC")
    List<StudentTitle> findByCampaignId(@Param("campaignId") UUID campaignId);
}
