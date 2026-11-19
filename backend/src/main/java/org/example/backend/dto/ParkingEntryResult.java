package org.example.backend.dto;

import java.time.OffsetDateTime;

/**
 * 入场结果，严格对应接口契约第 5 节。
 *
 * <p>{@code vehicleId} 已写入 {@code parking_record}，但不放在本响应中；成员 3 按
 * {@code id/recordNo} 读取记录即可拿到 {@code vehicle_id}。固定交接字段见
 * {@code docs/成员2_交付与合并操作说明.md}。</p>
 */
public class ParkingEntryResult {

    private Long id;
    private String recordNo;
    private Long lotId;
    private String licensePlate;
    private Long spaceId;
    private String areaCode;
    private String spaceCode;
    private Long feeRuleId;
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

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getSpaceCode() {
        return spaceCode;
    }

    public void setSpaceCode(String spaceCode) {
        this.spaceCode = spaceCode;
    }

    public Long getFeeRuleId() {
        return feeRuleId;
    }

    public void setFeeRuleId(Long feeRuleId) {
        this.feeRuleId = feeRuleId;
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
