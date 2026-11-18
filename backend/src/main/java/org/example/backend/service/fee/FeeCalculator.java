package org.example.backend.service.fee;

import java.math.BigDecimal;

/**
 * 纯计费函数（成员 3，01 契约第 4 节“费用纯函数”）。
 * 不访问数据库、不读系统时钟；三个显式输入 entryTime、exitTime、feeRule。
 * 时长分钟数按两时刻间秒数向上取整；不满或等于 freeMinutes 为 0；
 * 超过免费时先从总分钟扣免费分钟，对余量按连续的 1440 分钟周期分段，
 * 各段 min(ceil(段分钟 / billingUnitMinutes) * unitPrice, maxFeePer24h) 后相加。
 */
public final class FeeCalculator {

    private static final long MINUTES_PER_PERIOD = 1440L;

    private FeeCalculator() {
    }

    /** 两时刻间秒数向上取整至分钟（与 calculate 同一规则，供派生字段复用） */
    public static long ceilMinutes(java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        long seconds = java.time.Duration.between(from, to).getSeconds();
        return (seconds + 59) / 60;
    }

    public static FeeResult calculate(java.time.OffsetDateTime entryTime,
                                      java.time.OffsetDateTime exitTime,
                                      FeeRuleSnapshot rule) {
        if (entryTime == null || exitTime == null || rule == null) {
            throw new IllegalArgumentException("计费参数不完整：entryTime、exitTime、feeRule 均不能为空");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("离场时间不能早于入场时间");
        }
        long seconds = java.time.Duration.between(entryTime, exitTime).getSeconds();
        long totalMinutes = (seconds + 59) / 60; // 秒数向上取整至分钟
        if (totalMinutes <= rule.freeMinutes()) {
            return new FeeResult(totalMinutes, BigDecimal.ZERO.setScale(2));
        }
        long remaining = totalMinutes - rule.freeMinutes();
        int unit = rule.billingUnitMinutes();
        BigDecimal total = BigDecimal.ZERO;
        while (remaining > 0) {
            long periodMinutes = Math.min(remaining, MINUTES_PER_PERIOD);
            long units = (periodMinutes + unit - 1) / unit; // 段内向上取整到计费单位
            BigDecimal periodFee = rule.unitPrice().multiply(BigDecimal.valueOf(units));
            if (periodFee.compareTo(rule.maxFeePer24h()) > 0) {
                periodFee = rule.maxFeePer24h();
            }
            total = total.add(periodFee);
            remaining -= periodMinutes;
        }
        return new FeeResult(totalMinutes, total.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    /** 计费结果：时长分钟数与应收金额（两位小数） */
    public record FeeResult(long durationMinutes, BigDecimal amountDue) {
    }
}
