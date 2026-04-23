package com.sep490.ecoverse_be.dto.response.report;

import com.sep490.ecoverse_be.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentCoinReportResponse {

    private BigDecimal currentBalance;
    private BigDecimal totalEarnedInPeriod;
    private BigDecimal totalSpentInPeriod;
    private BigDecimal totalEarnedAllTime;
    private BigDecimal totalSpentAllTime;

    private List<CoinTransactionDto> transactions;

    private String period;
    private String fromDate;
    private String toDate;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CoinTransactionDto {
        private UUID id;
        private TransactionType transactionType;
        private BigDecimal amount;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private String description;
        private OffsetDateTime createdAt;
    }
}
