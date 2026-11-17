package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.OwnerVehicleCreateRequest;
import org.example.backend.dto.VehicleStatusRequest;
import org.example.backend.entity.Vehicle;
import org.example.backend.service.OwnerVehicleService;
import org.example.backend.support.PathIds;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车主本人车辆接口（OWNER）。归属只从会话取，请求体不接受 ownerUserId。
 */
@RestController
@RequestMapping("/api/owner/vehicles")
public class OwnerVehicleController {

    private final OwnerVehicleService ownerVehicleService;

    public OwnerVehicleController(OwnerVehicleService ownerVehicleService) {
        this.ownerVehicleService = ownerVehicleService;
    }

    @GetMapping
    public ApiResult<List<Vehicle>> list() {
        return ApiResult.ok(ownerVehicleService.list());
    }

    @PostMapping
    public ApiResult<Vehicle> create(@RequestBody OwnerVehicleCreateRequest request) {
        return ApiResult.ok(ownerVehicleService.create(request));
    }

    /** 只做启用/停用，不提供删除或换绑。 */
    @PatchMapping("/{id}/status")
    public ApiResult<Vehicle> changeStatus(@PathVariable("id") String id,
                                           @RequestBody VehicleStatusRequest request) {
        return ApiResult.ok(ownerVehicleService.changeStatus(PathIds.parse(id, "车辆 ID"), request));
    }
}
