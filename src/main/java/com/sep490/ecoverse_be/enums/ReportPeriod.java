package com.sep490.ecoverse_be.enums;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
    public static OffsetDateTime[] resolveDateRange(ReportPeriod period, OffsetDateTime fromDate, OffsetDateTime toDate) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now();
        OffsetDateTime start;
        OffsetDateTime end = now;

        switch (period) {
            case THIS_WEEK:
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
                break;
            case THIS_MONTH:
                start = today.withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
                break;
            case LAST_3_MONTHS:
                start = today.minusMonths(3).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
                break;
            case THIS_YEAR:
                start = today.withDayOfYear(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
                break;
            case CUSTOM:
                start = (fromDate != null) ? fromDate : today.withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
                end = (toDate != null) ? toDate : now;
                break;
            default:
                start = today.withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
        }

        return new OffsetDateTime[]{start, end};
    }
}
