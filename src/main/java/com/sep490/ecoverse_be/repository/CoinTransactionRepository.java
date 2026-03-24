package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    List<CoinTransaction> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
