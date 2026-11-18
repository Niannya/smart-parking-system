package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.ParkingAreaSaveRequest;
import org.example.backend.dto.ParkingAreaStatusRequest;
import org.example.backend.dto.ParkingLotOption;
import org.example.backend.entity.ParkingArea;
import org.example.backend.service.ParkingAreaService;
import org.springframework.web.bind.annotation.CrossOrigin;
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
 * 停车区域接口。
 *
 * <p>待办（依赖成员 1 的统一规范）：
 * 1. 本接口只允许角色为 ADMIN 的账号访问，目前鉴权基础尚未合并，先不加权限注解；
 * 2. 统一响应结构由成员 1 提供后，把 ApiResult 换成统一结构。</p>
 */
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/parking-areas")
public class ParkingAreaController {

    private final ParkingAreaService parkingAreaService;

    public ParkingAreaController(ParkingAreaService parkingAreaService) {
        this.parkingAreaService = parkingAreaService;
    }

    /** 按停车场列出区域；lotId 必填，status 可选（ACTIVE / DISABLED） */
    @GetMapping
    public ApiResult<List<ParkingArea>> list(@RequestParam(required = false) Long lotId,
                                             @RequestParam(required = false) String status) {
        return ApiResult.ok(parkingAreaService.list(lotId, status));
    }

    /** 停车场下拉选项（临时接口，等停车场模块合并后删除） */
    @GetMapping("/lots")
    public ApiResult<List<ParkingLotOption>> lots() {
        return ApiResult.ok(parkingAreaService.listLotOptions());
    }

    @PostMapping
    public ApiResult<ParkingArea> create(@RequestBody ParkingAreaSaveRequest request) {
        return ApiResult.ok(parkingAreaService.create(request));
    }

    /** 只修改名称和说明，不允许修改区域编号 */
    @PutMapping("/{id}")
    public ApiResult<ParkingArea> update(@PathVariable Long id,
                                        @RequestBody ParkingAreaSaveRequest request) {
        return ApiResult.ok(parkingAreaService.updateInfo(id, request));
    }

    /** 状态切换：{"status":"DISABLED"} 或 {"status":"ACTIVE"} */
    @PatchMapping("/{id}/status")
    public ApiResult<ParkingArea> changeStatus(@PathVariable Long id,
                                              @RequestBody ParkingAreaStatusRequest request) {
        return ApiResult.ok(parkingAreaService.changeStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        parkingAreaService.delete(id);
        return ApiResult.ok();
    }
}
