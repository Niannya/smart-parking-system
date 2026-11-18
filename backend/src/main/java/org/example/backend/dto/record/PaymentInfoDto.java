package org.example.backend.dto.record;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 支付信息（01 契约第 6 节 Record.payment）。无 payment 返回 null。
 */
public record PaymentInfoDto(
        String paymentNo,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        String paymentMethod,
        OffsetDateTime paymentTime) {
}
