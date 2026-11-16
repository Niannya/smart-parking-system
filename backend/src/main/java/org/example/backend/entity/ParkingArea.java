package org.example.backend.entity;

import java.time.OffsetDateTime;

/**
 * 停车区域，对应表 parking_area。
 *
 * <p>spaceCount 不是表字段，是列表查询时用子查询带出来的「车位数量」，只用于页面展示。</p>
 */
public class ParkingArea {

    private Long id;
    private Long parkingLotId;
    private String areaCode;
    private String areaName;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** 非表字段：该区域下的车位数量 */
    private Integer spaceCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public void setParkingLotId(Long parkingLotId) {
        this.parkingLotId = parkingLotId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getSpaceCount() {
        return spaceCount;
    }

    public void setSpaceCount(Integer spaceCount) {
        this.spaceCount = spaceCount;
    }
}
