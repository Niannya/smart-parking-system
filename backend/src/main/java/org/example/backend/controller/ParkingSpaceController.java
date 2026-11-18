package org.example.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.space.ParkingSpace;
import org.example.backend.dto.space.ParkingSpaceCreateRequest;
import org.example.backend.service.ParkingSpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 停车位（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@RestController
@RequestMapping("/api/parking-spaces")
@RequiredArgsConstructor
public class ParkingSpaceController {

    private final ParkingSpaceService spaceService;

    @GetMapping
    public ApiResult<List<ParkingSpace>> list(@RequestParam @NotNull @Positive(message = "lotId 必须为正整数") Long lotId,
                                              @RequestParam(required = false) Long areaId,
                                              @RequestParam(required = false) String status) {
        return ApiResult.ok(spaceService.list(lotId, areaId, status));
    }

    @PostMapping
    public ApiResult<ParkingSpace> create(@Valid @RequestBody ParkingSpaceCreateRequest request) {
        return ApiResult.ok(spaceService.create(request));
    }
}
