package org.example.backend.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定车辆请求（成员 2 scaffold）。ownerUserId 只从会话取。
 */
@Data
public class VehicleBindRequest {

    @NotBlank(message = "车牌不能为空")
    private String licensePlate;
}
