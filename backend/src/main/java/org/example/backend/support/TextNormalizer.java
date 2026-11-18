package org.example.backend.support;

import java.util.Locale;

/**
 * 文本规范化工具。
 *
 * <p>为什么必须有：数据库对业务编号设置了 CHECK 约束，例如
 * {@code ck_parking_area_code CHECK (area_code = UPPER(BTRIM(area_code)))}。
 * 如果后端把「a 区 」这样的输入直接入库，数据库会直接抛约束错误，前端只能看到 500。
 * 所以所有编号类字段统一在「入参进来时」和「入库前」各规范化一次。</p>
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * 规范化业务编号：去掉全部空白字符（含中间空格）并转大写。
     * 例：" a 01 " -> "A01"
     */
    public static String normalizeCode(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化普通文本：去掉首尾空白，中间连续空白压成一个空格。
     * 用于区域名称这类允许有空格的字段。
     */
    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }
}
