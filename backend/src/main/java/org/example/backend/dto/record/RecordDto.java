package org.example.backend.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 停车记录响应（01 契约第 6 节 Record）。
 * 管理员和车主使用同一可见 DTO，不返回内部 admin IDs。
 * durationMinutes 用同一向上取整规则；
 * PARKING 以查询时刻计算并提供 estimatedAmountDue、paymentStatus=UNPAID、payment=null；
 * COMPLETED 的 estimatedAmountDue=null，正常收款 paymentStatus=PAID，FREE_RELEASE 为 WAIVED；
 * ABNORMAL 为 ABNORMAL、payment=null。
 */
@Data
public class RecordDto {

    private Long id;
    private String recordNo;
    private Long lotId;
    private String lotName;
    private Long vehicleId;
    private String licensePlate;
    private Long spaceId;
    private String areaCode;
    private String spaceCode;
    private Long feeRuleId;
    private OffsetDateTime entryTime;
    private OffsetDateTime exitTime;
    private Long durationMinutes;
    private BigDecimal estimatedAmountDue;
    private String status;
    private String paymentStatus;
    private String abnormalReason;
    private PaymentInfoDto payment;
}
