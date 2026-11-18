package org.example.backend.dto.exit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 费用预览（01 契约第 5 节 Quote）。
 * 预览只供显示，不锁定最终金额；确认离场按服务器当时 exitTime 重算。
 */
public record QuoteDto(
        Long recordId,
        OffsetDateTime entryTime,
        OffsetDateTime quotedExitTime,
        long durationMinutes,
        BigDecimal amountDue,
        Long feeRuleId) {
}
