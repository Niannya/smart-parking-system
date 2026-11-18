package org.example.backend.service.fee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FeeCalculator 纯函数单元测试（成员 3，M2-06 / T11）。
 * 默认规则：30 分钟免费、60 分钟一单位、5.00 元、24 小时封顶 50.00。
 * 用固定时刻验证，不用 Thread.sleep。
 */
class FeeCalculatorTest {

    private static final OffsetDateTime ENTRY = OffsetDateTime.of(2026, 11, 16, 10, 0, 0, 0, ZoneOffset.ofHours(8));

    private static final FeeRuleSnapshot DEFAULT_RULE =
            new FeeRuleSnapshot(1L, "日常规则", 30, 60, new BigDecimal("5.00"), new BigDecimal("50.00"));

    private static FeeCalculator.FeeResult calc(long minutes) {
        return FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(minutes), DEFAULT_RULE);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    @Nested
    @DisplayName("免费时长边界（T11：0/30/31/60/61/91 分钟）")
    class FreeMinutesBoundary {

        @Test
        void zeroMinutesIsFree() {
            FeeCalculator.FeeResult result = calc(0);
            assertEquals(0, result.durationMinutes());
            assertEquals(money("0.00"), result.amountDue());
        }

        @Test
        void exactlyFreeMinutesIsFree() {
            assertEquals(money("0.00"), calc(30).amountDue());
        }

        @Test
        void oneMinuteOverFreeCostsOneUnit() {
            FeeCalculator.FeeResult result = calc(31);
            assertEquals(31, result.durationMinutes());
            assertEquals(money("5.00"), result.amountDue());
        }

        @Test
        void sixtyMinutesCostsOneUnit() {
            assertEquals(money("5.00"), calc(60).amountDue());
        }

        @Test
        void sixtyOneMinutesStillOneUnit() {
            // T11 校正：61 分钟扣 30 免费后剩 31 分钟，仍是一个计费单位
            FeeCalculator.FeeResult result = calc(61);
            assertEquals(61, result.durationMinutes());
            assertEquals(money("5.00"), result.amountDue());
        }

        @Test
        void ninetyOneMinutesRoundsUpToSecondUnit() {
            // 保留 91 分钟断言：验证向上进第二计费单位
            FeeCalculator.FeeResult result = calc(91);
            assertEquals(91, result.durationMinutes());
            assertEquals(money("10.00"), result.amountDue());
        }
    }

    @Nested
    @DisplayName("24 小时分段封顶（24h/26h/48h/49h）")
    class DailyCap {

        @Test
        void twentyFourHoursHitsCap() {
            assertEquals(money("50.00"), calc(24 * 60).amountDue());
        }

        @Test
        void twentySixHoursIsSixty() {
            // 24h 段封顶 50 + 余 90 分钟两个单位 10 = 60
            assertEquals(money("60.00"), calc(26 * 60).amountDue());
        }

        @Test
        void fortyEightHoursIsOneHundred() {
            assertEquals(money("100.00"), calc(48 * 60).amountDue());
        }

        @Test
        void fortyNineHoursIsOneHundredFive() {
            // 50 + 50 + 30 分钟一个单位 5
            assertEquals(money("105.00"), calc(49 * 60).amountDue());
        }
    }

    @Nested
    @DisplayName("规则参数变化")
    class RuleVariations {

        @Test
        void differentPriceAndFreeMinutes() {
            FeeRuleSnapshot rule = new FeeRuleSnapshot(2L, "夜间规则", 0, 30,
                    new BigDecimal("2.50"), new BigDecimal("80.00"));
            assertEquals(money("0.00"), FeeCalculator.calculate(ENTRY, ENTRY, rule).amountDue());
            assertEquals(money("2.50"), FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(1), rule).amountDue());
            assertEquals(money("2.50"), FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(30), rule).amountDue());
            assertEquals(money("5.00"), FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(31), rule).amountDue());
        }

        @Test
        void freeMinutesLongerThanStayIsFree() {
            FeeRuleSnapshot rule = new FeeRuleSnapshot(3L, "长免费", 600, 60,
                    new BigDecimal("5.00"), new BigDecimal("50.00"));
            assertEquals(money("0.00"), FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(599), rule).amountDue());
            // 第 601 分钟起计费：601-600=1 分钟 → 一个单位
            assertEquals(money("5.00"), FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(601), rule).amountDue());
        }

        @Test
        void zeroPriceCostsNothing() {
            FeeRuleSnapshot rule = new FeeRuleSnapshot(4L, "免费规则", 0, 60,
                    new BigDecimal("0.00"), new BigDecimal("0.00"));
            assertEquals(money("0.00"), FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(300), rule).amountDue());
        }
    }

    @Nested
    @DisplayName("时长秒数向上取整")
    class SecondsRounding {

        @Test
        void ninetySecondsCountsAsTwoMinutes() {
            FeeRuleSnapshot rule = new FeeRuleSnapshot(5L, "无免费", 0, 60,
                    new BigDecimal("5.00"), new BigDecimal("50.00"));
            FeeCalculator.FeeResult result =
                    FeeCalculator.calculate(ENTRY, ENTRY.plusSeconds(90), rule);
            assertEquals(2, result.durationMinutes());
            assertEquals(money("5.00"), result.amountDue());
        }

        @Test
        void fiftyNineSecondsCountsAsOneMinute() {
            FeeRuleSnapshot rule = new FeeRuleSnapshot(6L, "无免费", 0, 60,
                    new BigDecimal("5.00"), new BigDecimal("50.00"));
            FeeCalculator.FeeResult result =
                    FeeCalculator.calculate(ENTRY, ENTRY.plusSeconds(59), rule);
            assertEquals(1, result.durationMinutes());
            assertEquals(money("5.00"), result.amountDue());
        }

        @Test
        void thirtyMinutesFiftyNineSecondsIsOneUnitOverFree() {
            FeeCalculator.FeeResult result =
                    FeeCalculator.calculate(ENTRY, ENTRY.plusSeconds(30 * 60 + 59), DEFAULT_RULE);
            assertEquals(31, result.durationMinutes());
            assertEquals(money("5.00"), result.amountDue());
        }
    }

    @Nested
    @DisplayName("非法输入")
    class InvalidInput {

        @Test
        void exitBeforeEntryRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> FeeCalculator.calculate(ENTRY, ENTRY.minusMinutes(1), DEFAULT_RULE));
        }

        @Test
        void nullArgumentsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> FeeCalculator.calculate(null, ENTRY, DEFAULT_RULE));
            assertThrows(IllegalArgumentException.class,
                    () -> FeeCalculator.calculate(ENTRY, null, DEFAULT_RULE));
            assertThrows(IllegalArgumentException.class,
                    () -> FeeCalculator.calculate(ENTRY, ENTRY.plusMinutes(10), null));
        }
    }
}
