package org.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.example.backend.common.BusinessException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拒绝未知 JSON 字段的请求基类。
 *
 * <p>接口契约第 1 节要求「请求未知字段直接拒绝（400）」，尤其要挡住
 * {@code role / ownerUserId / entryAdminId / feeRuleId / entryTime / status} 这类
 * 服务端控制值。Jackson 默认会静默忽略未知字段，这里用 {@code @JsonAnySetter} 收集后显式拒绝。</p>
 */
public abstract class RejectUnknownJsonRequest {

    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void captureUnknownField(String name, Object value) {
        unknownFields.put(name, value);
    }

    /** 每个 Service 的入口第一步调用。 */
    public void rejectUnknownFields() {
        if (!unknownFields.isEmpty()) {
            throw new BusinessException(4000, "请求包含不支持的字段：" + String.join("、", unknownFields.keySet()));
        }
    }
}
