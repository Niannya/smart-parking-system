package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.dto.entry.EntryResultDto;
import org.example.backend.dto.entry.ParkingEntryRequest;
import org.example.backend.dto.exit.ActiveRecordDto;
import org.example.backend.dto.exit.AbnormalExitRequest;
import org.example.backend.dto.exit.ExitRequest;
import org.example.backend.dto.exit.ExitResultDto;
import org.example.backend.dto.exit.ParkingRecordRow;
import org.example.backend.dto.exit.PaymentInsert;
import org.example.backend.dto.exit.QuoteDto;
import org.example.backend.dto.feerule.FeeRuleCreateRequest;
import org.example.backend.mapper.ParkingExitMapper;
import org.example.backend.mapper.ParkingSpaceMapper;
import org.example.backend.support.PostgresIntegrationTest;
import org.example.backend.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

/**
 * 离场、模拟支付与异常通道数据库集成测试（成员 3，M4 / T16-T19）。
 * 本机无 PostgreSQL 时按假设跳过；复验命令见 PR 交付说明。
 */
class ParkingExitServiceTest extends PostgresIntegrationTest {

    @Autowired
    private ParkingExitService exitService;

    @Autowired
    private ParkingEntryService entryService;

    @Autowired
    private FeeRuleService feeRuleService;

    @Autowired
    private ParkingExitMapper exitMapper;

    @Autowired
    private ParkingSpaceMapper spaceMapper;

    @SpyBean
    private ParkingExitMapper parkingExitMapperSpy;

    /** 固定时钟：entry/quote/exit 的时间都可由测试控制，不等待真实时长 */
    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        TestSecurity.setAdmin(adminId);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.ofHours(8));
        lenient().when(clock.instant()).thenAnswer(invocation -> Instant.now());
    }

    @AfterEach
    void resetSpy() {
        // 避免注入失败的桩泄漏到同一 Spring 上下文的其他测试类
        Mockito.reset(parkingExitMapperSpy);
    }

    private void fixClockAt(Instant instant) {
        lenient().when(clock.instant()).thenReturn(instant);
    }

    private EntryResultDto enter(String plate, Instant entryInstant) {
        fixClockAt(entryInstant);
        ParkingEntryRequest request = new ParkingEntryRequest();
        request.setLotId(lot1Id);
        request.setLicensePlate(plate);
        return entryService.entry(request);
    }

    private ExitRequest cashRequest(BigDecimal amountPaid) {
        ExitRequest request = new ExitRequest();
        request.setPaymentMethod("CASH");
        request.setAmountPaid(amountPaid);
        return request;
    }

    private ExitRequest freeReleaseRequest(String remark) {
        ExitRequest request = new ExitRequest();
        request.setPaymentMethod("FREE_RELEASE");
        request.setAmountPaid(BigDecimal.ZERO.setScale(2));
        request.setRemark(remark);
        return request;
    }

    private AbnormalExitRequest abnormalRequest(String reason) {
        AbnormalExitRequest request = new AbnormalExitRequest();
        request.setReason(reason);
        return request;
    }

    private ParkingRecordRow recordRow(Long recordId) {
        return exitMapper.selectById(recordId);
    }

    private String spaceStatus(Long spaceId) {
        return spaceMapper.selectByFilter(lot1Id, null, null).stream()
                .filter(s -> s.getId().equals(spaceId))
                .findFirst()
                .map(org.example.backend.dto.space.ParkingSpace::getStatus)
                .orElse(null);
    }

    @Test
    @DisplayName("T16：切换场规则后，quote 与离场仍按记录绑定的旧规则计费")
    void quoteUsesRecordBoundRuleAfterRuleSwitch() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());

        // 切换该场活动规则（0 免费 / 60 单位 / 9 元）
        FeeRuleCreateRequest newRule = new FeeRuleCreateRequest();
        newRule.setLotId(lot1Id);
        newRule.setRuleName("切换后规则");
        newRule.setFreeMinutes(0);
        newRule.setBillingUnitMinutes(60);
        newRule.setUnitPrice(new BigDecimal("9.00"));
        newRule.setMaxFeePer24h(new BigDecimal("90.00"));
        feeRuleService.create(newRule);

        fixClockAt(T0.plusMinutes(61).toInstant());
        QuoteDto quote = exitService.quote(entry.getId());
        assertEquals(61, quote.durationMinutes());
        assertEquals(new BigDecimal("5.00"), quote.amountDue(), "旧规则：31 分钟计一个单位 5 元（若误用新规则则为 10 元）");
        assertEquals(entry.getFeeRuleId(), quote.feeRuleId());
    }

    @Test
    @DisplayName("T16：预览与确认跨分钟金额变化（61 分钟 5 元，91 分钟 10 元）")
    void amountChangesAcrossMinutes() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());

        fixClockAt(T0.plusMinutes(61).toInstant());
        QuoteDto quote61 = exitService.quote(entry.getId());
        assertEquals(new BigDecimal("5.00"), quote61.amountDue());

        // 用 61 分钟的金额在 91 分钟时确认应被拒绝（服务端按当时时刻重算）
        fixClockAt(T0.plusMinutes(91).toInstant());
        BusinessException mismatch = assertThrows(BusinessException.class,
                () -> exitService.exit(entry.getId(), cashRequest(new BigDecimal("5.00"))));
        assertEquals(ErrorCodes.EXIT_AMOUNT_MISMATCH, mismatch.getCode());

        ExitResultDto result = exitService.exit(entry.getId(), cashRequest(new BigDecimal("10.00")));
        assertEquals(new BigDecimal("10.00"), result.getAmountDue());
        assertEquals(91, result.getDurationMinutes());
    }

    @Test
    @DisplayName("T17：正常离场一次成功；重复 POST 409/4502；仅一条 payment")
    void normalExitThenDuplicateRejected() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());

        ExitResultDto result = exitService.exit(entry.getId(), cashRequest(new BigDecimal("5.00")));
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("FREE", result.getSpaceStatus());
        assertNotNull(result.getPaymentNo());
        assertTrue(result.getPaymentNo().startsWith("P"));
        assertEquals(32, result.getPaymentNo().length());
        assertEquals(new BigDecimal("5.00"), result.getAmountDue());
        assertEquals(new BigDecimal("5.00"), result.getAmountPaid());

        // 车位已释放、记录已完成
        assertEquals("FREE", spaceStatus(entry.getSpaceId()));
        assertEquals("COMPLETED", recordRow(entry.getId()).getStatus());
        assertEquals(1, exitMapper.countPaymentsByRecord(entry.getId()));

        // 重复离场：409/4502，不返回第二次成功
        BusinessException ex = assertThrows(BusinessException.class,
                () -> exitService.exit(entry.getId(), cashRequest(new BigDecimal("5.00"))));
        assertEquals(ErrorCodes.EXIT_ALREADY_COMPLETED, ex.getCode());
        assertEquals(1, exitMapper.countPaymentsByRecord(entry.getId()));

        // 已离场记录不能再预览
        BusinessException quoteEx = assertThrows(BusinessException.class,
                () -> exitService.quote(entry.getId()));
        assertEquals(ErrorCodes.EXIT_ALREADY_COMPLETED, quoteEx.getCode());
    }

    @Test
    @DisplayName("T18：实收金额不匹配 4503，记录仍在场、车位仍占用、无支付")
    void amountMismatchKeepsRecordParking() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> exitService.exit(entry.getId(), cashRequest(new BigDecimal("6.00"))));
        assertEquals(ErrorCodes.EXIT_AMOUNT_MISMATCH, ex.getCode());

        assertEquals("PARKING", recordRow(entry.getId()).getStatus());
        assertEquals("OCCUPIED", spaceStatus(entry.getSpaceId()));
        assertEquals(0, exitMapper.countPaymentsByRecord(entry.getId()));
    }

    @Test
    @DisplayName("T18：免费放行缺原因 4504；带原因时完成并保留原应收额")
    void freeReleaseRequiresRemarkAndKeepsAmountDue() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());

        BusinessException missing = assertThrows(BusinessException.class,
                () -> exitService.exit(entry.getId(), freeReleaseRequest(null)));
        assertEquals(ErrorCodes.EXIT_FREE_RELEASE_REASON_REQUIRED, missing.getCode());
        assertEquals("PARKING", recordRow(entry.getId()).getStatus());

        BusinessException nonZero = assertThrows(BusinessException.class, () -> {
            ExitRequest request = freeReleaseRequest("人工减免");
            request.setAmountPaid(new BigDecimal("1.00"));
            exitService.exit(entry.getId(), request);
        });
        assertEquals(ErrorCodes.EXIT_AMOUNT_MISMATCH, nonZero.getCode());

        ExitResultDto result = exitService.exit(entry.getId(), freeReleaseRequest("人工核实后减免"));
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(new BigDecimal("5.00"), result.getAmountDue(), "保留原 amountDue 便于核对减免");
        assertEquals(new BigDecimal("0.00"), result.getAmountPaid());
        assertEquals("FREE", spaceStatus(entry.getSpaceId()));
    }

    @Test
    @DisplayName("T19：异常离场置 ABNORMAL、不生成支付、释放车位；重复操作 4502")
    void abnormalExitSetsAbnormalWithoutPayment() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());

        ExitResultDto result = exitService.abnormal(entry.getId(), abnormalRequest("道闸故障人工放行"));
        assertEquals("ABNORMAL", result.getStatus());
        assertNull(result.getPaymentNo());
        assertNull(result.getAmountPaid());
        assertEquals(new BigDecimal("5.00"), result.getAmountDue(), "amountDue 仅作核对参考");
        assertEquals("FREE", result.getSpaceStatus());

        ParkingRecordRow row = recordRow(entry.getId());
        assertEquals("ABNORMAL", row.getStatus());
        assertEquals("道闸故障人工放行",
                jdbcTemplate.queryForObject("SELECT abnormal_reason FROM parking_record WHERE id = ?",
                        String.class, entry.getId()));
        assertEquals(0, exitMapper.countPaymentsByRecord(entry.getId()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> exitService.abnormal(entry.getId(), abnormalRequest("再试一次")));
        assertEquals(ErrorCodes.EXIT_ALREADY_COMPLETED, ex.getCode());
    }

    @Test
    @DisplayName("异常离场原因必填")
    void abnormalExitRequiresReason() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> exitService.abnormal(entry.getId(), abnormalRequest("  ")));
        assertEquals(ErrorCodes.BAD_REQUEST, ex.getCode());
        assertEquals("PARKING", recordRow(entry.getId()).getStatus());
    }

    @Test
    @DisplayName("T18：支付插入失败时三处全部回滚（无支付、记录 PARKING、车位 OCCUPIED）")
    void rollbackWhenPaymentInsertFails() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());

        doThrow(new RuntimeException("模拟支付插入失败")).when(parkingExitMapperSpy).insertPayment(any(PaymentInsert.class));

        assertThrows(RuntimeException.class,
                () -> exitService.exit(entry.getId(), cashRequest(new BigDecimal("5.00"))));

        assertEquals(0, exitMapper.countPaymentsByRecord(entry.getId()));
        assertEquals("PARKING", recordRow(entry.getId()).getStatus());
        assertEquals("OCCUPIED", spaceStatus(entry.getSpaceId()));
    }

    @Test
    @DisplayName("T18：车位释放失败时全部回滚（记录回到 PARKING、无支付）")
    void rollbackWhenSpaceReleaseFails() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());

        doThrow(new RuntimeException("模拟车位释放失败")).when(parkingExitMapperSpy).releaseSpace(entry.getSpaceId());

        assertThrows(RuntimeException.class,
                () -> exitService.exit(entry.getId(), cashRequest(new BigDecimal("5.00"))));

        assertEquals("PARKING", recordRow(entry.getId()).getStatus());
        assertEquals(0, exitMapper.countPaymentsByRecord(entry.getId()));
        assertEquals("OCCUPIED", spaceStatus(entry.getSpaceId()));
    }

    @Test
    @DisplayName("在场查询：车牌/记录号恰填一个；车牌规范化；无记录 4501")
    void findActiveValidation() {
        String plate = randomPlate();
        EntryResultDto entry = enter(plate, T0.toInstant());

        // 小写车牌也能查到（规范化）
        ActiveRecordDto byLowerPlate = exitService.findActive(plate.toLowerCase(), null);
        assertEquals(entry.getId(), byLowerPlate.getId());
        assertEquals(plate, byLowerPlate.getLicensePlate());

        ActiveRecordDto byRecordNo = exitService.findActive(null, entry.getRecordNo());
        assertEquals(entry.getId(), byRecordNo.getId());

        // 两个都填 / 都不填
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> exitService.findActive(plate, entry.getRecordNo())).getCode());
        assertEquals(ErrorCodes.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> exitService.findActive(null, null)).getCode());

        // 无在场记录
        assertEquals(ErrorCodes.EXIT_NO_ACTIVE_RECORD, assertThrows(BusinessException.class,
                () -> exitService.findActive(randomPlate(), null)).getCode());
        assertEquals(ErrorCodes.EXIT_NO_ACTIVE_RECORD, assertThrows(BusinessException.class,
                () -> exitService.findActive(null, "R" + "0".repeat(31))).getCode());

        // 车牌格式错误
        assertEquals(ErrorCodes.VEHICLE_PLATE_INVALID, assertThrows(BusinessException.class,
                () -> exitService.findActive("不是车牌", null)).getCode());
    }

    @Test
    @DisplayName("离场后同一车牌可再次入场（全局同牌在场约束只在 PARKING 期间生效）")
    void plateCanEnterAgainAfterExit() {
        String plate = randomPlate();
        EntryResultDto first = enter(plate, T0.toInstant());
        fixClockAt(T0.plusMinutes(61).toInstant());
        exitService.exit(first.getId(), cashRequest(new BigDecimal("5.00")));

        EntryResultDto second = enter(plate, T0.plusMinutes(120).toInstant());
        assertNotEquals(first.getId(), second.getId());
        assertEquals("PARKING", recordRow(second.getId()).getStatus());
    }

    @Test
    @DisplayName("支付编号唯一且格式合规（P + 31 位）")
    void paymentNoFormat() {
        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String plate = randomPlate();
            EntryResultDto entry = enter(plate, T0.toInstant());
            fixClockAt(T0.plusMinutes(61).toInstant());
            ExitResultDto result = exitService.exit(entry.getId(), cashRequest(new BigDecimal("5.00")));
            numbers.add(result.getPaymentNo());
            assertTrue(result.getPaymentNo().matches("^P[0-9a-f]{31}$"));
        }
        assertEquals(3, numbers.stream().distinct().count());
    }
}
