package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.entry.EntryResultDto;
import org.example.backend.dto.entry.ParkingEntryRequest;
import org.example.backend.service.ParkingEntryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆入场（成员 2 scaffold：最小可用实现，正式实现由成员 2 负责）。
 */
@RestController
@RequestMapping("/api/parking-entries")
@RequiredArgsConstructor
public class ParkingEntryController {

    private final ParkingEntryService entryService;

    @PostMapping
    public ApiResult<EntryResultDto> entry(@Valid @RequestBody ParkingEntryRequest request) {
        return ApiResult.ok(entryService.entry(request));
    }
}
