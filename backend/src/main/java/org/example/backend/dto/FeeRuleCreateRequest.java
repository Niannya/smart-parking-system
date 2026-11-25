package org.example.backend.dto;

import java.math.BigDecimal;

/**
 * 新建收费规则请求（01 契约第 4 节）。创建即激活；
 * 历史规则不可改删，因此没有修改/删除请求体。
 *
 * <p>该校验由 Service 手工完成（与停车场/区域模块一致），不使用 Bean Validation：
 * 数据库已有 ck_fee_rule_values 等约束兜底，应用层先给出可读错误。</p>
 */
public class FeeRuleCreateRequest {

    private Long lotId;
    private String ruleName;
    private Integer freeMinutes;
    private Integer billingUnitMinutes;
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

    public Integer getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(Integer freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public Integer getBillingUnitMinutes() {
        return billingUnitMinutes;
    }

    public void setBillingUnitMinutes(Integer billingUnitMinutes) {
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
}
