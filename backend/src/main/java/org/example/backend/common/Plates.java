package org.example.backend.common;

import org.example.backend.common.ErrorCodes;

/**
 * 车牌规范化与格式校验（01 契约第 1 节）。
 * 课程项目约定的普通小型车基础校验，不声称覆盖全部真实车牌种类。
 */
public final class Plates {

    private Plates() {
    }

    private static final String PLATE_PATTERN =
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z][A-HJ-NP-Z0-9]{5,6}$";

    /** trim 后转大写；格式不符抛 400/4204 */
    public static String normalize(String plate) {
        if (plate == null) {
            throw BusinessException.badRequest(ErrorCodes.VEHICLE_PLATE_INVALID, "车牌不能为空");
        }
        String normalized = plate.trim().toUpperCase();
        if (!normalized.matches(PLATE_PATTERN)) {
            throw BusinessException.badRequest(ErrorCodes.VEHICLE_PLATE_INVALID, "车牌格式错误");
        }
        return normalized;
    }

    /** 仅 trim + 大写，不做格式校验（查询入参宽松场景） */
    public static String normalizeLoose(String plate) {
        return plate == null ? null : plate.trim().toUpperCase();
    }
}
