package org.example.backend.dto.space;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 停车位（成员 2 scaffold）。parkingLotId/areaCode 来自 JOIN，不在表内重复存储。
 */
@Data
public class ParkingSpace {

    private Long id;
    private Long areaId;
    private Long parkingLotId;
    private String areaCode;
    private String spaceCode;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
