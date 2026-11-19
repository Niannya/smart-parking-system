package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.ParkingEntryRequest;
import org.example.backend.dto.ParkingEntryResult;
import org.example.backend.service.ParkingEntryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆入场接口（ADMIN）。
 *
 * <p>只做请求绑定和统一响应包装；权限、车牌、事务、同牌、规则、选位、占位、记录
 * 全部在 {@link ParkingEntryService} 内完成。</p>
 */
@RestController
@RequestMapping("/api/parking-entries")
public class ParkingEntryController {

    private final ParkingEntryService parkingEntryService;

    public ParkingEntryController(ParkingEntryService parkingEntryService) {
        this.parkingEntryService = parkingEntryService;
    }

    @PostMapping
    public ApiResult<ParkingEntryResult> enter(@RequestBody ParkingEntryRequest request) {
        return ApiResult.ok(parkingEntryService.enter(request));
    }
}
