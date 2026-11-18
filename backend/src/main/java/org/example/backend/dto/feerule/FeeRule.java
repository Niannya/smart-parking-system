package org.example.backend.dto.feerule;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 收费规则（实体与响应同一形态，字段即 01 契约 FeeRule）。
 */
@Data
public class FeeRule {

    private Long id;
    private Long parkingLotId;
    private String ruleName;
    private Integer freeMinutes;
    private Integer billingUnitMinutes;
    private BigDecimal unitPrice;
    private BigDecimal maxFeePer24h;
    private Boolean isActive;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
