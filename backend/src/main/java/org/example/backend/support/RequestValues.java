package org.example.backend.support;

import org.example.backend.common.BusinessException;

import java.util.Locale;

/**
 * 请求字段的通用校验与规范化。
 *
 * <p>所有校验都在服务端做（接口契约第 1 节：请求字段由后端再次校验），
 * 错误统一用 4000（参数通用错误），业务码由各模块自己决定。</p>
 */
public final class RequestValues {

    private RequestValues() {
    }

    public static Long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(4000, message);
        }
        return value;
    }

    /** 业务编号：去空白 + 转大写，必填，长度受数据库列限制。 */
    public static String requireCode(String value, String fieldName, int maxLength) {
        String normalized = TextNormalizer.normalizeCode(value);
        if (normalized.isEmpty()) {
            throw new BusinessException(4000, fieldName + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(4000, fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    /** 普通文本：去首尾空白、压缩中间空白，必填。 */
    public static String requireText(String value, String fieldName, int maxLength) {
        String normalized = TextNormalizer.normalizeText(value);
        if (normalized.isEmpty()) {
            throw new BusinessException(4000, fieldName + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(4000, fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    /** 可选说明：空白视为 null。 */
    public static String normalizeDescription(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(4000, "说明不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    /**
     * 解析状态枚举。
     *
     * @param status    请求值
     * @param nullable  是否允许省略（列表过滤时为 true）
     * @param allowed   允许的枚举值
     */
    public static String parseStatus(String status, boolean nullable, String... allowed) {
        if (status == null) {
            if (nullable) {
                return null;
            }
            throw new BusinessException(4000, "状态不能为空");
        }
        // 契约第 1 节：字段省略表示不过滤，空字符串属于非法过滤值
        if (status.isBlank()) {
            throw new BusinessException(4000, "状态不能为空字符串");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        for (String candidate : allowed) {
            if (candidate.equals(normalized)) {
                return normalized;
            }
        }
        throw new BusinessException(4000, "状态只能是 " + String.join(" 或 ", allowed));
    }
}
