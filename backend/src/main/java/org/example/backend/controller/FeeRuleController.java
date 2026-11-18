package org.example.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.example.backend.dto.feerule.FeeRule;
import org.example.backend.dto.feerule.FeeRuleCreateRequest;
import org.example.backend.service.FeeRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收费规则（成员 3，ADMIN）。路径 /api/fee-rules/** 仅 ADMIN（SecurityConfig）。
 */
@RestController
@RequestMapping("/api/fee-rules")
@RequiredArgsConstructor
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    /** 每场规则历史列表，按 createdAt DESC, id DESC */
    @GetMapping
    public ApiResult<List<FeeRule>> list(@RequestParam @NotNull @Positive(message = "lotId 必须为正整数") Long lotId) {
        return ApiResult.ok(feeRuleService.listByLot(lotId));
    }

    /** 当前生效规则；无规则 404/4403 */
    @GetMapping("/active")
    public ApiResult<FeeRule> active(@RequestParam @NotNull @Positive(message = "lotId 必须为正整数") Long lotId) {
        return ApiResult.ok(feeRuleService.getActive(lotId));
    }

    /** 创建即激活；历史规则不可改删，无 PUT/DELETE */
    @PostMapping
    public ApiResult<FeeRule> create(@Valid @RequestBody FeeRuleCreateRequest request) {
        return ApiResult.ok(feeRuleService.create(request));
    }
}
