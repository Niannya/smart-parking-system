package org.example.backend.dto;

import java.time.OffsetDateTime;

public final class ParkingLotDto {

    private ParkingLotDto() {
    }

    public record View(Long id, String lotCode, String lotName, String address, String description,
                       String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record Option(Long id, String lotCode, String lotName) {
    }

    public record CreateRequest(String lotCode, String lotName, String address, String description) {
    }

    public record UpdateRequest(String lotName, String address, String description) {
    }

    public record StatusRequest(String status) {
    }
}
