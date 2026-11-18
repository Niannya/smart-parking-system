package org.example.backend.dto.entry;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 停车记录插入参数（成员 2 scaffold：入场写入的数据事实，
 * 成员 3 离场与查询只读这些字段：lotId、spaceId、feeRuleId、vehicleId、licensePlate、entryTime、status）。
 */
@Data
public class ParkingRecordInsert {

    private Long id;
    private String recordNo;
    private Long parkingLotId;
    private Long parkingSpaceId;
    private Long feeRuleId;
    private Long vehicleId;
    private String licensePlate;
    private Long entryAdminId;
    private OffsetDateTime entryTime;
}
