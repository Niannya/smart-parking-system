package org.example.backend.entity;

import java.time.OffsetDateTime;

/**
 * 停车记录，对应表 parking_record。
 *
 * <p>本实体由成员 2 维护“入场创建”字段；成员 3 的离场/查询只读本表并建自己的
 * Mapper，不修改本实体。字段变更先按 01 契约沟通。</p>
 */
public class ParkingRecord {

    private Long id;
    private String recordNo;
    private Long parkingLotId;
    private Long vehicleId;
    private String licensePlate;
    private Long parkingSpaceId;
    private Long feeRuleId;
    private Long entryAdminId;
    private OffsetDateTime entryTime;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public void setParkingLotId(Long parkingLotId) {
        this.parkingLotId = parkingLotId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Long getParkingSpaceId() {
        return parkingSpaceId;
    }

    public void setParkingSpaceId(Long parkingSpaceId) {
        this.parkingSpaceId = parkingSpaceId;
    }

    public Long getFeeRuleId() {
        return feeRuleId;
    }

    public void setFeeRuleId(Long feeRuleId) {
        this.feeRuleId = feeRuleId;
    }

    public Long getEntryAdminId() {
        return entryAdminId;
    }

    public void setEntryAdminId(Long entryAdminId) {
        this.entryAdminId = entryAdminId;
    }

    public OffsetDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(OffsetDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
