package org.example.backend.dto.lot;

/**
 * 场地选项（01 契约：GET /api/parking-lots/options 返回 {"id","lotCode","lotName"}）。
 */
public record ParkingLotOption(Long id, String lotCode, String lotName) {
}
