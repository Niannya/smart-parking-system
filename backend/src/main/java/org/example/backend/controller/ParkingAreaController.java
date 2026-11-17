package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.ParkingAreaCreateRequest;
import org.example.backend.dto.ParkingAreaStatusRequest;
import org.example.backend.dto.ParkingAreaUpdateRequest;
import org.example.backend.entity.ParkingArea;
import org.example.backend.service.ParkingAreaService;
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
 * 停车区域接口（ADMIN）。
 *
 * <p>权限的双重保障：成员 1 在安全过滤链上限制 {@code /api/parking-areas/**} 仅 ADMIN；
 * 本模块 Service 内再校验一次角色，避免只剩前端隐藏菜单。</p>
 */
@RestController
@RequestMapping("/api/parking-areas")
public class ParkingAreaController {

    private final ParkingAreaService parkingAreaService;

    public ParkingAreaController(ParkingAreaService parkingAreaService) {
        this.parkingAreaService = parkingAreaService;
    }

    /** 按停车场列出区域；lotId 必填，status 可选（ACTIVE / DISABLED）。 */
    @GetMapping
    public ApiResult<List<ParkingArea>> list(@RequestParam(required = false) Long lotId,
                                             @RequestParam(required = false) String status) {
        return ApiResult.ok(parkingAreaService.list(lotId, status));
    }

    @PostMapping
    public ApiResult<ParkingArea> create(@RequestBody ParkingAreaCreateRequest request) {
        return ApiResult.ok(parkingAreaService.create(request));
    }

    /** 只修改名称和说明，不允许修改区域编号与归属。 */
    @PutMapping("/{id}")
    public ApiResult<ParkingArea> update(@PathVariable("id") String id,
                                         @RequestBody ParkingAreaUpdateRequest request) {
        return ApiResult.ok(parkingAreaService.updateInfo(PathIds.parse(id, "区域 ID"), request));
    }

    /** 状态切换：{"status":"DISABLED"} 或 {"status":"ACTIVE"}。 */
    @PatchMapping("/{id}/status")
    public ApiResult<ParkingArea> changeStatus(@PathVariable("id") String id,
                                               @RequestBody ParkingAreaStatusRequest request) {
        return ApiResult.ok(parkingAreaService.changeStatus(PathIds.parse(id, "区域 ID"), request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") String id) {
        parkingAreaService.delete(PathIds.parse(id, "区域 ID"));
        return ApiResult.ok();
    }
}
