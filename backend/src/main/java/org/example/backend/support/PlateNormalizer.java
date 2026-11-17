package org.example.backend.support;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 车牌规范化与格式校验。
 *
 * <p>规则来自接口契约第 1 节：普通小型车基础校验，不声称覆盖全部真实车牌种类。
 * 入库前必须「去空白 + 转大写」，否则会直接违反数据库的 {@code ck_*_license_plate} 约束。</p>
 */
public final class PlateNormalizer {

    /** 省份简称 + 发牌机关代号 + 5～6 位（不含 I、O，避免与数字混淆）。 */
    private static final Pattern PLATE_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]"
                    + "[A-Z][A-HJ-NP-Z0-9]{5,6}$");

    private PlateNormalizer() {
    }

    /** 去掉所有空白字符并转大写；输入为 null 时返回空串。 */
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** 校验已经规范化后的车牌。 */
    public static boolean isValid(String normalizedPlate) {
        return normalizedPlate != null && PLATE_PATTERN.matcher(normalizedPlate).matches();
    }
}
