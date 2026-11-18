package org.example.backend.dto.entry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 入场请求（成员 2 scaffold）。
 */
@Data
public class ParkingEntryRequest {

    @NotNull(message = "停车场不能为空")
    @Positive(message = "lotId 必须为正整数")
    private Long lotId;

    @NotBlank(message = "车牌不能为空")
    private String licensePlate;
}
