package org.example.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 收费规则，对应表 fee_rule；字段即 01 契约 FeeRule 响应形态。
 *
 * <p>每个停车场同一时刻最多一条 is_active=true 的规则，由数据库唯一部分索引
 * {@code uk_fee_rule_one_active_per_lot} 保证。历史规则只读：不提供修改/删除，
 * 停车记录保留入场时绑定的规则 ID。</p>
 */
public class FeeRule {

    private Long id;
    private Long parkingLotId;
    private String ruleName;
    private Integer freeMinutes;
    private Integer billingUnitMinutes;
    private BigDecimal unitPrice;
    private BigDecimal maxFeePer24h;
    private Boolean isActive;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private Long createdBy;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public void setParkingLotId(Long parkingLotId) {
        this.parkingLotId = parkingLotId;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(OffsetDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public OffsetDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(OffsetDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
