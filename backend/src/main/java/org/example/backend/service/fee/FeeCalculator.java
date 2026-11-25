package org.example.backend.service.fee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 纯计费函数（成员 3，01 契约第 4 节“费用纯函数”）。
 * 不访问数据库、不读系统时钟；三个显式输入 entryTime、exitTime、feeRule。
 * 时长分钟数按两时刻间时长（含亚秒）向上取整；不满或等于 freeMinutes 为 0；
 * 超过免费时先从总分钟扣免费分钟，对余量按连续的 1440 分钟周期分段，
 * 各段 min(ceil(段分钟 / billingUnitMinutes) * unitPrice, maxFeePer24h) 后相加。
 */
public final class FeeCalculator {

    private static final long MINUTES_PER_PERIOD = 1440L;

    private FeeCalculator() {
    }

    /**
     * 两时刻间时长向上取整至分钟，保留亚秒精度（与 calculate 同一规则，供派生字段复用）。
     *
     * <p>不能直接用 {@code Duration.getSeconds()}：它截断亚秒，刚过整分钟的时刻
     * （如免费 30 分钟时停了 30 分 0.5 秒）会被算成 30 分钟、少收一档；
     * 契约要求向上进到 31 分钟。实现按秒、纳秒分别处理，无溢出风险：
     * 设秒数 s = 60q + r（0 &le; r &lt; 60）、亚秒部分 f，则
     * ceil((s+f)/60) = q + (r &gt; 0 或 f &gt; 0 ? 1 : 0)。</p>
     */
    public static long ceilMinutes(OffsetDateTime from, OffsetDateTime to) {
        Duration duration = Duration.between(from, to);
        long seconds = duration.getSeconds();
        long quotient = Math.floorDiv(seconds, 60L);
        long remainder = Math.floorMod(seconds, 60L);
        return quotient + ((remainder > 0L || duration.getNano() > 0) ? 1L : 0L);
    }

    public static FeeResult calculate(OffsetDateTime entryTime,
                                      OffsetDateTime exitTime,
                                      FeeRuleSnapshot rule) {
        if (entryTime == null || exitTime == null || rule == null) {
            throw new IllegalArgumentException("计费参数不完整：entryTime、exitTime、feeRule 均不能为空");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("离场时间不能早于入场时间");
        }
        long totalMinutes = ceilMinutes(entryTime, exitTime); // 时长向上取整至分钟（含亚秒精度）
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
        return new FeeResult(totalMinutes, total.setScale(2, RoundingMode.HALF_UP));
    }

    /** 计费结果：时长分钟数与应收金额（两位小数） */
    public record FeeResult(long durationMinutes, BigDecimal amountDue) {
    }
}
