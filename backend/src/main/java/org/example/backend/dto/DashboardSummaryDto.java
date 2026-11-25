package org.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryDto(
        Long lotId,
        LocalDate date,
        long totalSpaces,
        long freeSpaces,
        long occupiedSpaces,
        long disabledSpaces,
        BigDecimal occupancyRate,
        long entryCount,
        long exitCount,
        BigDecimal totalRevenue
) {
}
