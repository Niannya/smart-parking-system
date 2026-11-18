package org.example.backend.dto.record;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 停车记录查询行（成员 3 内部行：JOIN 所属场、空间，LEFT JOIN payment_record）。
 */
@Data
public class RecordRow {

    private Long id;
    private String recordNo;
    private Long parkingLotId;
    private String lotName;
    private Long vehicleId;
    private String licensePlate;
    private Long parkingSpaceId;
    private String areaCode;
    private String spaceCode;
    private Long feeRuleId;
    private OffsetDateTime entryTime;
    private OffsetDateTime exitTime;
    private String status;
    private String abnormalReason;
    // LEFT JOIN payment_record（可为空）
    private String paymentNo;
    private java.math.BigDecimal amountDue;
    private java.math.BigDecimal amountPaid;
    private String paymentMethod;
    private OffsetDateTime paymentTime;
}
