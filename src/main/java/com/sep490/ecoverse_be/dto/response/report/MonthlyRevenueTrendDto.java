package com.sep490.ecoverse_be.dto.response.report;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyRevenueTrendDto {

    private int year;
    private int month;
    private BigDecimal totalRevenue;
    private String label;
}
