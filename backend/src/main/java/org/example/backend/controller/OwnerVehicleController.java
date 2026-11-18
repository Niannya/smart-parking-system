package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.vehicle.Vehicle;
import org.example.backend.dto.vehicle.VehicleBindRequest;
import org.example.backend.service.OwnerVehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 本人车辆（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@RestController
@RequestMapping("/api/owner/vehicles")
@RequiredArgsConstructor
public class OwnerVehicleController {

    private final OwnerVehicleService vehicleService;

    @GetMapping
    public ApiResult<List<Vehicle>> listOwn() {
        return ApiResult.ok(vehicleService.listOwn());
    }

    @PostMapping
    public ApiResult<Vehicle> bind(@Valid @RequestBody VehicleBindRequest request) {
        return ApiResult.ok(vehicleService.bind(request));
    }

    @PatchMapping("/{id}/status")
    public ApiResult<Vehicle> updateStatus(@PathVariable Long id,
                                           @RequestBody java.util.Map<String, String> body) {
        return ApiResult.ok(vehicleService.updateStatus(id, body.get("status")));
    }
}
