package org.example.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.exit.ActiveRecordDto;
import org.example.backend.dto.exit.ExitRequest;
import org.example.backend.dto.exit.ExitResultDto;
import org.example.backend.dto.exit.QuoteDto;
import org.example.backend.service.ParkingExitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在场查询、费用预览、正常离场与异常离场（成员 3，ADMIN）。
 * 路径 /api/parking-exits/** 仅 ADMIN（SecurityConfig）。
 */
@RestController
@RequestMapping("/api/parking-exits")
@RequiredArgsConstructor
public class ParkingExitController {

    private final ParkingExitService parkingExitService;

    /** 按车牌或记录编号查在场记录；二者恰填一个，无则 404/4501 */
    @GetMapping("/active")
    public ApiResult<ActiveRecordDto> active(@RequestParam(required = false) String licensePlate,
                                             @RequestParam(required = false) String recordNo) {
        return ApiResult.ok(parkingExitService.findActive(licensePlate, recordNo));
    }

    /** 费用预览：只接受 PARKING；预览不锁定最终金额 */
    @GetMapping("/{recordId}/quote")
    public ApiResult<QuoteDto> quote(@PathVariable @NotNull @Positive(message = "recordId 必须为正整数") Long recordId) {
        return ApiResult.ok(parkingExitService.quote(recordId));
    }

    /** 正常离场 + 模拟确认收款；重复 POST 409/4502 */
    @PostMapping("/{recordId}")
    public ApiResult<ExitResultDto> exit(@PathVariable @NotNull @Positive(message = "recordId 必须为正整数") Long recordId,
                                         @Valid @RequestBody ExitRequest request) {
        return ApiResult.ok(parkingExitService.exit(recordId, request));
    }

    /** 异常离场：原因必填，不生成支付记录；重复操作 409/4502 */
    @PostMapping("/{recordId}/abnormal")
    public ApiResult<ExitResultDto> abnormal(@PathVariable @NotNull @Positive(message = "recordId 必须为正整数") Long recordId,
                                             @Valid @RequestBody org.example.backend.dto.exit.AbnormalExitRequest request) {
        return ApiResult.ok(parkingExitService.abnormal(recordId, request));
    }
}
