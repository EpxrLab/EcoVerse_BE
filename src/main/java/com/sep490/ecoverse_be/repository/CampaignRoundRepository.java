package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRound;
import com.sep490.ecoverse_be.enums.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRoundRepository extends JpaRepository<CampaignRound, UUID> {

    List<CampaignRound> findByCampaignIdOrderByRoundNumberAsc(UUID campaignId);

    Optional<CampaignRound> findByIdAndCampaignId(UUID id, UUID campaignId);

    Optional<CampaignRound> findByCampaignIdAndRoundNumber(UUID campaignId, Integer roundNumber);

        @Query("""
                        SELECT r
                        FROM CampaignRound r
                        JOIN r.campaign c
                        WHERE r.status = :status
                            AND r.startTime <= :now
                            AND c.isActive = true
                            AND (
                                (c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'ON_GOING')
                                OR
                                (c.campaignType <> 'PARTNERSHIP_EVENT' AND c.schoolStatus = 'ON_GOING')
                            )
                        """)
        List<CampaignRound> findRoundsReadyToActivate(@Param("status") RoundStatus status,
                                                                                                    @Param("now") OffsetDateTime now);

        @Query("""
                        SELECT r
                        FROM CampaignRound r
                        JOIN r.campaign c
                        WHERE r.status = :status
                            AND r.endTime <= :now
                            AND c.isActive = true
                            AND (
                                (c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'ON_GOING')
                                OR
                                (c.campaignType <> 'PARTNERSHIP_EVENT' AND c.schoolStatus = 'ON_GOING')
                            )
                        """)
        List<CampaignRound> findRoundsReadyToComplete(@Param("status") RoundStatus status,
                                                                                                    @Param("now") OffsetDateTime now);
}

