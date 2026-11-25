package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.DashboardSummaryDto;
import org.example.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    public ApiResult<DashboardSummaryDto> summary(
            @RequestParam(name = "lotId", required = false) String lotId,
            @RequestParam(name = "date", required = false) String date) {
        return ApiResult.ok(dashboard.summary(lotId, date));
    }
}
