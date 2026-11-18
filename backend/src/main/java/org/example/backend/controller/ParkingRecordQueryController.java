package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.dto.record.RecordDto;
import org.example.backend.dto.record.RecordFilter;
import org.example.backend.service.ParkingRecordQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 停车记录查询（成员 3）。
 * 管理员：/api/parking-records；车主：/api/owner/parking-records。
 * 路径权限由 SecurityConfig 强制：/api/parking-records/** 仅 ADMIN，/api/owner/** 仅 OWNER。
 */
@RestController
@RequiredArgsConstructor
public class ParkingRecordQueryController {

    private final ParkingRecordQueryService recordQueryService;

    /** 管理员列表：lotId/licensePlate/status/spaceId/from/to 均可省；按 entryTime DESC, id DESC */
    @GetMapping("/api/parking-records")
    public ApiResult<List<RecordDto>> list(@RequestParam(required = false) Long lotId,
                                           @RequestParam(required = false) String licensePlate,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long spaceId,
                                           @RequestParam(required = false) OffsetDateTime from,
                                           @RequestParam(required = false) OffsetDateTime to) {
        RecordFilter filter = new RecordFilter();
        filter.setLotId(requirePositive(lotId, "lotId"));
        filter.setLicensePlate(licensePlate);
        filter.setStatus(status);
        filter.setSpaceId(requirePositive(spaceId, "spaceId"));
        filter.setEntryFrom(from);
        filter.setEntryTo(to);
        return ApiResult.ok(recordQueryService.listForAdmin(filter));
    }

    /** 管理员详情 */
    @GetMapping("/api/parking-records/{id}")
    public ApiResult<RecordDto> detail(@PathVariable Long id) {
        requirePositive(id, "id");
        return ApiResult.ok(recordQueryService.getForAdmin(id));
    }

    /** 车主列表：只返回本人绑定车辆关联的记录；status 可省 */
    @GetMapping("/api/owner/parking-records")
    public ApiResult<List<RecordDto>> listOwn(@RequestParam(required = false) String status) {
        return ApiResult.ok(recordQueryService.listForOwner(status));
    }

    /** 车主详情：非本人 403/4602 */
    @GetMapping("/api/owner/parking-records/{id}")
    public ApiResult<RecordDto> detailOwn(@PathVariable Long id) {
        requirePositive(id, "id");
        return ApiResult.ok(recordQueryService.getForOwner(id));
    }

    private Long requirePositive(Long value, String name) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw BusinessException.badRequest(ErrorCodes.BAD_REQUEST, name + " 必须为正整数");
        }
        return value;
    }
}
