package org.example.backend.dto.exit;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 停车记录行（成员 3 内部查询行，JOIN 区域/车位码）。
 * 成员 2 入场写入的数据事实：lotId、spaceId、feeRuleId、vehicleId、licensePlate、entryTime、status=PARKING。
 */
@Data
public class ParkingRecordRow {

    private Long id;
    private String recordNo;
    private Long parkingLotId;
    private Long parkingSpaceId;
    private Long feeRuleId;
    private Long vehicleId;
    private String licensePlate;
    private OffsetDateTime entryTime;
    private String status;
    private String areaCode;
    private String spaceCode;
}
