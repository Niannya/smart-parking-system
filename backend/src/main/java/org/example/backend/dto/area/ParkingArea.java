package org.example.backend.dto.area;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 停车区域（成员 2 scaffold）。
 */
@Data
public class ParkingArea {

    private Long id;
    private Long parkingLotId;
    private String areaCode;
    private String areaName;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
