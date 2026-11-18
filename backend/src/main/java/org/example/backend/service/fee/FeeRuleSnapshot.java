package org.example.backend.service.fee;

import java.math.BigDecimal;

/**
 * 计费规则快照：只含计费所需字段，供 FeeCalculator 与入场/离场服务使用。
 */
public record FeeRuleSnapshot(Long feeRuleId,
                              String ruleName,
                              int freeMinutes,
                              int billingUnitMinutes,
                              BigDecimal unitPrice,
                              BigDecimal maxFeePer24h) {
}
