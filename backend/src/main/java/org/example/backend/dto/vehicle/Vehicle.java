package org.example.backend.dto.vehicle;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 车主车辆（成员 2 scaffold）。
 */
@Data
public class Vehicle {

    private Long id;
    private Long ownerUserId;
    private String licensePlate;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
