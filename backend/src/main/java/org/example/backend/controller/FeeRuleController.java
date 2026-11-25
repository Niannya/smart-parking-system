package org.example.backend.controller;

import org.example.backend.common.ApiResult;
import org.example.backend.dto.FeeRuleCreateRequest;
import org.example.backend.entity.FeeRule;
import org.example.backend.service.FeeRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收费规则接口（成员 3，ADMIN）。路径 /api/fee-rules/** 仅 ADMIN（SecurityConfig 已配置）。
 *
 * <p>历史规则不可改删，停车记录保留入场时绑定的规则 ID，因此没有 PUT/DELETE。
 * 成员 2 的入场服务不经过本控制器，直接依赖 {@code FeeRuleQuery.findActiveForLot}。</p>
 */
@RestController
@RequestMapping("/api/fee-rules")
public class FeeRuleController {

    private final FeeRuleService service;

    public FeeRuleController(FeeRuleService service) {
        this.service = service;
    }

    /** 每场规则历史列表，按 createdAt DESC, id DESC；lotId 必填 */
    @GetMapping
    public ApiResult<List<FeeRule>> list(@RequestParam(name = "lotId", required = false) Long lotId) {
        return ApiResult.ok(service.listByLot(lotId));
    }

    /** 当前生效规则；无规则 404/4403 */
    @GetMapping("/active")
    public ApiResult<FeeRule> active(@RequestParam(name = "lotId", required = false) Long lotId) {
        return ApiResult.ok(service.getActive(lotId));
    }

    /** 创建即激活；旧规则转历史，新规则立即生效 */
    @PostMapping
    public ApiResult<FeeRule> create(@RequestBody FeeRuleCreateRequest request) {
        return ApiResult.ok(service.create(request));
    }
}
