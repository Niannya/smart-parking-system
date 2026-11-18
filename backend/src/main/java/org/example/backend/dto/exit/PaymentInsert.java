package org.example.backend.dto.exit;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 支付记录插入参数（内部使用，不对客户端暴露）。
 */
@Data
public class PaymentInsert {

    private String paymentNo;
    private Long parkingRecordId;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private OffsetDateTime paymentTime;
    private String remark;
    private Long handledBy;
}
