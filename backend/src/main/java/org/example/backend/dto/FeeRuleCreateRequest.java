package org.example.backend.dto;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;

import java.math.BigDecimal;

/**
 * 新建收费规则请求（01 契约第 4 节）。创建即激活；
 * 历史规则不可改删，因此没有修改/删除请求体。
 *
 * <p>该校验由 Service 手工完成（与停车场/区域模块一致），不使用 Bean Validation：
 * 数据库已有 ck_fee_rule_values 等约束兜底，应用层先给出可读错误。</p>
 *
 * <p>免费时长/计费单位用 {@code BigDecimal} 接收而非 {@code Integer}：
 * Jackson 默认开启 ACCEPT_FLOAT_AS_INT，JSON 小数（如 30.9）会被静默截断成 30，
 * 之后的校验看不到原始小数，管理员会存下与输入不同的规则。
 * 因此在反序列化后的校验阶段（{@link #requireFreeMinutes(int)}、
 * {@link #requireBillingUnitMinutes(int)}）显式拒绝小数，错误信息带回原始输入。</p>
 */
public class FeeRuleCreateRequest {

    private Long lotId;
    private String ruleName;
    private BigDecimal freeMinutes;
    private BigDecimal billingUnitMinutes;
    private BigDecimal unitPrice;
    private BigDecimal maxFeePer24h;

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public BigDecimal getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(BigDecimal freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public BigDecimal getBillingUnitMinutes() {
        return billingUnitMinutes;
    }

    public void setBillingUnitMinutes(BigDecimal billingUnitMinutes) {
        this.billingUnitMinutes = billingUnitMinutes;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getMaxFeePer24h() {
        return maxFeePer24h;
    }

    public void setMaxFeePer24h(BigDecimal maxFeePer24h) {
        this.maxFeePer24h = maxFeePer24h;
    }

    /**
     * 校验并取得免费时长（整数分钟）：非空、不小于 min、必须是整数。
     * 30.0 / 60.00 这类数值为整数的写法接受；30.9 拒绝（4402）。
     */
    public int requireFreeMinutes(int min) {
        return requireIntegerMinutes(this.freeMinutes, "免费时长", min);
    }

    /**
     * 校验并取得计费单位（整数分钟）：非空、不小于 min、必须是整数。
     */
    public int requireBillingUnitMinutes(int min) {
        return requireIntegerMinutes(this.billingUnitMinutes, "计费单位分钟", min);
    }

    private static int requireIntegerMinutes(BigDecimal value, String label, int min) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE, label + "不能为空");
        }
        if (value.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE,
                    label + "必须是整数分钟，不支持小数（收到 " + value.toPlainString() + "）");
        }
        int result;
        try {
            result = value.intValueExact();
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE, label + "超出可表示范围");
        }
        if (result < min) {
            throw new BusinessException(ErrorCode.INVALID_FEE_RULE,
                    label + (min <= 0 ? "不能为负" : "必须为正数"));
        }
        return result;
    }
}
