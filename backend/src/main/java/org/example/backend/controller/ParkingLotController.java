package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.lot.ParkingLotOption;
import org.example.backend.mapper.ParkingLotMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 停车场（成员 1 scaffold：仅实现联调链路所需的最小只读集合）。
 * 路径 /api/parking-lots/** 仅 ADMIN（SecurityConfig）。
 */
@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotMapper parkingLotMapper;

    /** 场地选项：只取 ACTIVE，按 lotCode 升序（供前端场地选择器） */
    @GetMapping("/options")
    public ApiResult<List<ParkingLotOption>> options() {
        return ApiResult.ok(parkingLotMapper.selectOptions());
    }
}
