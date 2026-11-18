package org.example.backend.dto.exit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 离场结果（01 契约第 5 节 ExitResult）。
 * 异常离场 paymentNo 为 null、amountPaid 为 null、不生成支付记录、默认不计收入。
 */
@Data
@AllArgsConstructor
public class ExitResultDto {

    private Long recordId;
    private String paymentNo;
    private OffsetDateTime exitTime;
    private long durationMinutes;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String status;
    private String spaceStatus;
}
