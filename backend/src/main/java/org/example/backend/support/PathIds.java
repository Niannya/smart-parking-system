package org.example.backend.support;

import org.example.backend.common.BusinessException;

/**
 * 路径 ID 解析：契约要求路径 ID 必须是正整数，非数字、0、负数一律按参数错误 4000 处理，
 * 避免框架抛类型转换异常后被当成 500。
 */
public final class PathIds {

    private PathIds() {
    }

    public static Long parse(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BusinessException(4000, fieldName + " 不能为空");
        }
        try {
            long value = Long.parseLong(rawValue.trim());
            if (value <= 0) {
                throw new BusinessException(4000, fieldName + " 必须为正整数");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new BusinessException(4000, fieldName + " 必须为正整数");
        }
    }
}
