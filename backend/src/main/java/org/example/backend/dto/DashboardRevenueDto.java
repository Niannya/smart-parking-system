package org.example.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardRevenueDto(
        Long lotId,
        OffsetDateTime from,
        OffsetDateTime to,
        long paymentCount,
        BigDecimal totalRevenue
) {
}
