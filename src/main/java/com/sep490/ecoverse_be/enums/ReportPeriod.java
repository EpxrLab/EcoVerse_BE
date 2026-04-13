package com.sep490.ecoverse_be.enums;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

public enum ReportPeriod {
    THIS_WEEK,
    THIS_MONTH,
    LAST_3_MONTHS,
    THIS_YEAR,
    CUSTOM;

    /**
     * Resolves the [from, to] date range for the given period.
     * For CUSTOM period, fromDate and toDate must be provided; defaults to THIS_MONTH if null.
     */
    public static LocalDateTime[] resolveDateRange(ReportPeriod period, LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end = now;

        switch (period) {
            case THIS_WEEK:
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                break;
            case THIS_MONTH:
                start = today.withDayOfMonth(1).atStartOfDay();
                break;
            case LAST_3_MONTHS:
                start = today.minusMonths(3).atStartOfDay();
                break;
            case THIS_YEAR:
                start = today.withDayOfYear(1).atStartOfDay();
                break;
            case CUSTOM:
                start = (fromDate != null) ? fromDate : today.withDayOfMonth(1).atStartOfDay();
                end = (toDate != null) ? toDate : now;
                break;
            default:
                start = today.withDayOfMonth(1).atStartOfDay();
        }

        return new LocalDateTime[]{start, end};
    }
}
