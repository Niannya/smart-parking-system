package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.ParkingLotDto;
import org.example.backend.service.ParkingLotService;
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

@RestController
@RequestMapping("/api/parking-lots")
public class ParkingLotController {

    private final ParkingLotService service;

    public ParkingLotController(ParkingLotService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<ParkingLotDto.View>> list(@RequestParam(name = "status", required = false) String status) {
        return ApiResult.ok(service.list(status));
    }

    @GetMapping("/options")
    public ApiResult<List<ParkingLotDto.Option>> options() {
        return ApiResult.ok(service.options());
    }

    @PostMapping
    public ApiResult<ParkingLotDto.View> create(@RequestBody ParkingLotDto.CreateRequest request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<ParkingLotDto.View> update(@PathVariable("id") Long id,
                                                @RequestBody ParkingLotDto.UpdateRequest request) {
        return ApiResult.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResult<ParkingLotDto.View> status(@PathVariable("id") Long id,
                                                @RequestBody ParkingLotDto.StatusRequest request) {
        return ApiResult.ok(service.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ApiResult.ok();
    }
}
