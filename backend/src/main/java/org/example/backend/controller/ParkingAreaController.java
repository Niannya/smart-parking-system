package org.example.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.area.ParkingArea;
import org.example.backend.dto.area.ParkingAreaCreateRequest;
import org.example.backend.service.ParkingAreaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 停车区域（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@RestController
@RequestMapping("/api/parking-areas")
@RequiredArgsConstructor
public class ParkingAreaController {

    private final ParkingAreaService areaService;

    @GetMapping
    public ApiResult<List<ParkingArea>> list(@RequestParam @NotNull @Positive(message = "lotId 必须为正整数") Long lotId) {
        return ApiResult.ok(areaService.listByLot(lotId));
    }

    @PostMapping
    public ApiResult<ParkingArea> create(@Valid @RequestBody ParkingAreaCreateRequest request) {
        return ApiResult.ok(areaService.create(request));
    }
}
