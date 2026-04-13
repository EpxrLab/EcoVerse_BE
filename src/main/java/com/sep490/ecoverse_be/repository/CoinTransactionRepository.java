package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CoinTransaction;
import com.sep490.ecoverse_be.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    List<CoinTransaction> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT SUM(ct.amount) FROM CoinTransaction ct WHERE ct.student.id = :studentId AND ct.transactionType IN :types")
    BigDecimal sumByStudentIdAndTypes(@Param("studentId") UUID studentId, @Param("types") List<TransactionType> types);

    @Query("SELECT SUM(ct.amount) FROM CoinTransaction ct WHERE ct.student.id = :studentId AND ct.transactionType IN :types AND ct.createdAt BETWEEN :from AND :to")
    BigDecimal sumByStudentIdAndTypesAndDateRange(@Param("studentId") UUID studentId, @Param("types") List<TransactionType> types, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT ct FROM CoinTransaction ct WHERE ct.student.id = :studentId AND ct.createdAt BETWEEN :from AND :to ORDER BY ct.createdAt DESC")
    List<CoinTransaction> findByStudentIdAndDateRange(@Param("studentId") UUID studentId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
