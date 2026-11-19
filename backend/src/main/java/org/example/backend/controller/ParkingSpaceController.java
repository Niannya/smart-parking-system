package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.ParkingSpaceCreateRequest;
import org.example.backend.dto.ParkingSpaceStatusRequest;
import org.example.backend.dto.ParkingSpaceUpdateRequest;
import org.example.backend.entity.ParkingSpace;
import org.example.backend.service.ParkingSpaceService;
import org.example.backend.support.PathIds;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 停车位接口（ADMIN）。归属由后端 JOIN 反查，客户端传错 lotId/areaId 不会被信任。
 */
@RestController
@RequestMapping("/api/parking-spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;

    public ParkingSpaceController(ParkingSpaceService parkingSpaceService) {
        this.parkingSpaceService = parkingSpaceService;
    }

    /** lotId 必填；areaId / status 可省；按区域码、车位码升序。 */
    @GetMapping
    public ApiResult<List<ParkingSpace>> list(@RequestParam(required = false) Long lotId,
                                              @RequestParam(required = false) Long areaId,
                                              @RequestParam(required = false) String status) {
        return ApiResult.ok(parkingSpaceService.list(lotId, areaId, status));
    }

    @PostMapping
    public ApiResult<ParkingSpace> create(@RequestBody ParkingSpaceCreateRequest request) {
        return ApiResult.ok(parkingSpaceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<ParkingSpace> update(@PathVariable("id") String id,
                                          @RequestBody ParkingSpaceUpdateRequest request) {
        return ApiResult.ok(parkingSpaceService.updateInfo(PathIds.parse(id, "车位 ID"), request));
    }

    /** 只允许 FREE / DISABLED；OCCUPIED 只能由入场/离场事务写入。 */
    @PatchMapping("/{id}/status")
    public ApiResult<ParkingSpace> changeStatus(@PathVariable("id") String id,
                                                @RequestBody ParkingSpaceStatusRequest request) {
        return ApiResult.ok(parkingSpaceService.changeStatus(PathIds.parse(id, "车位 ID"), request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") String id) {
        parkingSpaceService.delete(PathIds.parse(id, "车位 ID"));
        return ApiResult.ok();
    }
}
