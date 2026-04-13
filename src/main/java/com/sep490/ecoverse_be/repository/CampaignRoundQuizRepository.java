package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRoundQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRoundQuizRepository extends JpaRepository<CampaignRoundQuiz, UUID> {

    // Lay tat ca quiz cua 1 round theo thu tu hien thi
    List<CampaignRoundQuiz> findByCampaignRoundIdOrderByDisplayOrderAsc(UUID roundId);

    // Kiem tra quiz co thuoc round khong
    Optional<CampaignRoundQuiz> findByCampaignRoundIdAndQuizId(UUID roundId, UUID quizId);

    // Dem so quiz trong round
    long countByCampaignRoundId(UUID roundId);

    // Kiem tra quiz da ton tai trong round chua
    boolean existsByCampaignRoundIdAndQuizId(UUID roundId, UUID quizId);

    // Xoa toan bo quiz cua 1 round (dung cho overwrite)
    void deleteByCampaignRoundId(UUID roundId);
}
