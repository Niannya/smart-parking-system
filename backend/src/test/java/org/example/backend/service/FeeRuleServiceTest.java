package org.example.backend.service;

import org.example.backend.dto.feerule.FeeRule;
import org.example.backend.dto.feerule.FeeRuleCreateRequest;
import org.example.backend.service.fee.FeeRuleQuery;
import org.example.backend.service.fee.FeeRuleSnapshot;
import org.example.backend.support.PostgresIntegrationTest;
import org.example.backend.support.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 收费规则服务数据库集成测试（成员 3，M2-05 / T10）。
 * 本机无 PostgreSQL 时按假设跳过；复验命令见 PR 交付说明。
 */
class FeeRuleServiceTest extends PostgresIntegrationTest {

    @Autowired
    private FeeRuleService feeRuleService;

    @Autowired
    private FeeRuleQuery feeRuleQuery;

    @BeforeEach
    void loginAdmin() {
        TestSecurity.setAdmin(adminId);
    }

    private FeeRuleCreateRequest request(Long lotId, String name, int freeMinutes, int unitMinutes,
                                         String price, String cap) {
        FeeRuleCreateRequest request = new FeeRuleCreateRequest();
        request.setLotId(lotId);
        request.setRuleName(name);
        request.setFreeMinutes(freeMinutes);
        request.setBillingUnitMinutes(unitMinutes);
        request.setUnitPrice(new BigDecimal(price));
        request.setMaxFeePer24h(new BigDecimal(cap));
        return request;
    }

    @Test
    @DisplayName("T10：场 A 建新规则后仅新规则 active，旧规则转历史；场 B 不变")
    void createActivatesNewAndKeepsHistory() {
        FeeRule created = feeRuleService.create(request(lot1Id, "切换后规则", 0, 60, "9.00", "90.00"));

        assertTrue(created.getIsActive());
        assertNotNull(created.getId());
        assertEquals(adminId, created.getCreatedBy());

        List<FeeRule> lot1Rules = feeRuleService.listByLot(lot1Id);
        assertEquals(2, lot1Rules.size());
        assertEquals(created.getId(), lot1Rules.get(0).getId(), "按 createdAt DESC, id DESC 排序，新规则在前");
        FeeRule old = lot1Rules.get(1);
        assertFalse(old.getIsActive(), "旧规则转历史");
        assertNotNull(old.getEffectiveTo(), "旧规则 effective_to 已写");

        FeeRule active = feeRuleService.getActive(lot1Id);
        assertEquals(created.getId(), active.getId());

        FeeRule lot2Active = feeRuleService.getActive(lot2Id);
        assertEquals("日常规则", lot2Active.getRuleName(), "不同场规则互不影响");
        assertEquals(1, feeRuleService.listByLot(lot2Id).size());
    }

    @Test
    @DisplayName("同场最多一条活动规则")
    void onlyOneActiveRulePerLot() {
        feeRuleService.create(request(lot1Id, "规则二", 0, 60, "9.00", "90.00"));
        feeRuleService.create(request(lot1Id, "规则三", 15, 30, "2.00", "20.00"));

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM fee_rule WHERE parking_lot_id = ? AND is_active = true", Integer.class, lot1Id);
        assertEquals(1, activeCount);

        FeeRule active = feeRuleService.getActive(lot1Id);
        assertEquals("规则三", active.getRuleName());
    }

    @Test
    @DisplayName("无活动规则的停车场：findActiveForLot 抛 4303（成员 2 入场据此拒绝）")
    void findActiveForLotThrowsWhenNoRule() {
        Long lot3Id = jdbcTemplate.queryForObject(
                "INSERT INTO parking_lot (lot_code, lot_name, status) VALUES (?, ?, 'ACTIVE') RETURNING id",
                Long.class, randomLotCode(), "测试三场");

        org.example.backend.common.BusinessException ex = assertThrows(
                org.example.backend.common.BusinessException.class,
                () -> feeRuleQuery.findActiveForLot(lot3Id));
        assertEquals(4303, ex.getCode());

        // 建规则后即可取到
        feeRuleService.create(request(lot3Id, "三场规则", 30, 60, "5.00", "50.00"));
        FeeRuleSnapshot snapshot = feeRuleQuery.findActiveForLot(lot3Id);
        assertEquals("三场规则", snapshot.ruleName());
    }

    @Test
    @DisplayName("停车场不存在：2001")
    void createRejectsUnknownLot() {
        org.example.backend.common.BusinessException ex = assertThrows(
                org.example.backend.common.BusinessException.class,
                () -> feeRuleService.create(request(999999999L, "幽灵规则", 0, 60, "1.00", "10.00")));
        assertEquals(2001, ex.getCode());
    }

    @Test
    @DisplayName("停车场已停用：2005")
    void createRejectsDisabledLot() {
        Long disabledLotId = jdbcTemplate.queryForObject(
                "INSERT INTO parking_lot (lot_code, lot_name, status) VALUES (?, ?, 'DISABLED') RETURNING id",
                Long.class, randomLotCode(), "停用场");
        org.example.backend.common.BusinessException ex = assertThrows(
                org.example.backend.common.BusinessException.class,
                () -> feeRuleService.create(request(disabledLotId, "停用场规则", 0, 60, "1.00", "10.00")));
        assertEquals(2005, ex.getCode());
    }

    @Test
    @DisplayName("历史规则保留且可被旧记录继续绑定计费（快照按 ID 读取）")
    void historicalRuleSnapshotStillReadable() {
        FeeRule first = feeRuleService.create(request(lot1Id, "首版规则", 0, 60, "3.00", "30.00"));
        feeRuleService.create(request(lot1Id, "二版规则", 30, 60, "5.00", "50.00"));

        FeeRuleSnapshot snapshot = feeRuleQuery.getSnapshotById(first.getId());
        assertEquals("首版规则", snapshot.ruleName());
        assertEquals(0, snapshot.freeMinutes());
        assertEquals(new BigDecimal("3.00"), snapshot.unitPrice());

        assertThrows(org.example.backend.common.BusinessException.class,
                () -> feeRuleQuery.getSnapshotById(999999999L));
    }
}
