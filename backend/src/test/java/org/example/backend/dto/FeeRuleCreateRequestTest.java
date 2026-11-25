package org.example.backend.dto;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FeeRuleCreateRequest 整数分钟校验单测（review 修复）。
 *
 * <p>背景：Jackson 默认 ACCEPT_FLOAT_AS_INT，若 DTO 用 Integer 接收，
 * JSON 里的 30.9 会被静默截断成 30，校验层看不到原始小数。
 * 现字段用 BigDecimal 接收，在 requireXxxMinutes 校验阶段显式拒绝小数。</p>
 */
class FeeRuleCreateRequestTest {

    private static FeeRuleCreateRequest requestWith(String freeMinutes, String billingUnitMinutes) {
        FeeRuleCreateRequest request = new FeeRuleCreateRequest();
        request.setFreeMinutes(freeMinutes == null ? null : new BigDecimal(freeMinutes));
        request.setBillingUnitMinutes(billingUnitMinutes == null ? null : new BigDecimal(billingUnitMinutes));
        return request;
    }

    private static void expectInvalidFeeRule(Runnable action) {
        BusinessException e = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.INVALID_FEE_RULE.getCode(), e.getCode());
    }

    @Nested
    @DisplayName("小数拒绝（不得静默截断）")
    class DecimalsRejected {

        @Test
        void freeMinutesWithDecimalRejected() {
            expectInvalidFeeRule(() -> requestWith("30.9", "60").requireFreeMinutes(0));
        }

        @Test
        void billingUnitMinutesWithDecimalRejected() {
            expectInvalidFeeRule(() -> requestWith("30", "0.5").requireBillingUnitMinutes(1));
        }

        @Test
        void tinyDecimalRejected() {
            expectInvalidFeeRule(() -> requestWith("30.0001", "60").requireFreeMinutes(0));
        }
    }

    @Nested
    @DisplayName("整数写法接受")
    class IntegersAccepted {

        @Test
        void plainIntegers() {
            assertEquals(30, requestWith("30", "60").requireFreeMinutes(0));
            assertEquals(60, requestWith("30", "60").requireBillingUnitMinutes(1));
        }

        @Test
        void trailingZeroScaleIsStillInteger() {
            // 30.0 / 60.00 的数值就是整数，接受
            assertEquals(30, requestWith("30.0", "60.00").requireFreeMinutes(0));
            assertEquals(60, requestWith("30.0", "60.00").requireBillingUnitMinutes(1));
        }

        @Test
        void zeroFreeMinutesAllowed() {
            assertEquals(0, requestWith("0", "60").requireFreeMinutes(0));
        }
    }

    @Nested
    @DisplayName("空值与范围")
    class NullAndRange {

        @Test
        void nullRejected() {
            expectInvalidFeeRule(() -> requestWith(null, "60").requireFreeMinutes(0));
            expectInvalidFeeRule(() -> requestWith("30", null).requireBillingUnitMinutes(1));
        }

        @Test
        void negativeRejected() {
            expectInvalidFeeRule(() -> requestWith("-1", "60").requireFreeMinutes(0));
        }

        @Test
        void billingUnitZeroRejected() {
            expectInvalidFeeRule(() -> requestWith("30", "0").requireBillingUnitMinutes(1));
        }

        @Test
        void outOfIntRangeRejected() {
            expectInvalidFeeRule(() -> requestWith("99999999999", "60").requireFreeMinutes(0));
        }
    }
}
